package com.projekfajar.ongkir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.alamat.service.AlamatOngkirResolver;
import com.projekfajar.ongkir.dto.HasilOngkir;
import com.projekfajar.ongkir.dto.RajaOngkirCostResponse;
import com.projekfajar.ongkir.model.Ongkir;
import com.projekfajar.ongkir.repository.OngkirRepository;
import com.projekfajar.settings.service.SettingService;

/**
 * RajaOngkir gagal dengan cara apa pun harus selalu jatuh ke tarif tetap
 * secara diam-diam -- kalau tidak, pembeli bisa melihat error karena masalah
 * pihak ketiga yang seharusnya tidak pernah sampai ke mereka.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OngkirCalculationServiceTest {

    @Mock
    private AlamatOngkirResolver alamatOngkirResolver;

    @Mock
    private RajaOngkirClient rajaOngkirClient;

    @Mock
    private OngkirRepository ongkirRepository;

    @Mock
    private SettingService settingService;

    @InjectMocks
    private OngkirCalculationService ongkirCalculationService;

    private Alamat alamat;

    @BeforeEach
    void setUp() {
        alamat = Alamat.builder()
                .id(1L)
                .provinsi("DKI Jakarta")
                .kota("Jakarta Selatan")
                .kecamatan("Kebayoran Baru")
                .kelurahan("Senayan")
                .kodePos("12190")
                .build();

        when(settingService.getInt("shipping.gratis-ongkir-minimal", 0)).thenReturn(0);
    }

    private RajaOngkirCostResponse.Tarif tarif(String kode, long biaya, String etd) {
        return tarif(kode, "REG", biaya, etd);
    }

    private RajaOngkirCostResponse.Tarif tarif(String kode, String layanan, long biaya, String etd) {
        RajaOngkirCostResponse.Tarif t = new RajaOngkirCostResponse.Tarif();
        t.setCode(kode);
        t.setName(kode.toUpperCase());
        t.setService(layanan);
        t.setDescription(layanan);
        t.setCost(BigDecimal.valueOf(biaya));
        t.setEtd(etd);
        return t;
    }

    private void mockRajaOngkirDuaOpsi() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn("501");
        when(alamatOngkirResolver.resolveDestinationId(alamat)).thenReturn(Optional.of("574"));
        when(settingService.getList("shipping.rajaongkir-kurir")).thenReturn(List.of("jne", "tiki"));
        when(rajaOngkirClient.hitungOngkir("501", "574", 500, List.of("jne", "tiki")))
                .thenReturn(Optional.of(List.of(
                        tarif("jne", "YES", 25000, "1 day"),
                        tarif("tiki", "REG", 12000, "3-4 day"))));
    }

    @Test
    @DisplayName("RajaOngkir berhasil dipakai dan mengambil opsi termurah")
    void rajaOngkirSuksesAmbilTermurah() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn("501");
        when(alamatOngkirResolver.resolveDestinationId(alamat)).thenReturn(Optional.of("574"));
        when(settingService.getList("shipping.rajaongkir-kurir")).thenReturn(List.of("jne", "tiki"));
        when(rajaOngkirClient.hitungOngkir("501", "574", 500, List.of("jne", "tiki")))
                .thenReturn(Optional.of(List.of(
                        tarif("jne", 15000, "2-3 day"),
                        tarif("tiki", 12000, "3-4 day"))));

        HasilOngkir hasil = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(1_000_000));

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_RAJAONGKIR);
        assertThat(hasil.getTarif()).isEqualByComparingTo("12000");
        assertThat(hasil.getEstimasiHari()).isEqualTo(4);
        assertThat(hasil.getKurirCode()).isEqualTo("tiki");
        assertThat(hasil.getLayanan()).isEqualTo("REG");
        assertThat(hasil.getOpsi()).hasSize(2);
        verify(ongkirRepository, never()).findByProvinsi(any());
    }

    @Test
    @DisplayName("Alamat gagal dicocokkan ke lokasi RajaOngkir -> jatuh ke tarif tetap")
    void alamatGagalDicocokkanJatuhKeTarifTetap() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn("501");
        when(alamatOngkirResolver.resolveDestinationId(alamat)).thenReturn(Optional.empty());
        when(ongkirRepository.findByProvinsi("DKI Jakarta"))
                .thenReturn(Optional.of(Ongkir.builder()
                        .provinsi("DKI Jakarta")
                        .tarif(new BigDecimal("15000.00"))
                        .estimasiHari(2)
                        .build()));

        HasilOngkir hasil = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(1_000_000));

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_TARIF_TETAP);
        assertThat(hasil.getTarif()).isEqualByComparingTo("15000.00");
        assertThat(hasil.getOpsi()).isEmpty();
        verify(rajaOngkirClient, never()).hitungOngkir(any(), any(), anyInt(), anyList());
    }

    @Test
    @DisplayName("RajaOngkir gagal/timeout/kuota habis -> jatuh ke tarif tetap tanpa error ke pemanggil")
    void rajaOngkirGagalJatuhKeTarifTetap() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn("501");
        when(alamatOngkirResolver.resolveDestinationId(alamat)).thenReturn(Optional.of("574"));
        when(settingService.getList("shipping.rajaongkir-kurir")).thenReturn(List.of("jne"));
        when(rajaOngkirClient.hitungOngkir(any(), any(), anyInt(), anyList())).thenReturn(Optional.empty());
        when(ongkirRepository.findByProvinsi("DKI Jakarta")).thenReturn(Optional.empty());

        HasilOngkir hasil = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(1_000_000));

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_TARIF_TETAP);
        assertThat(hasil.getTarif()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Lokasi asal belum diatur admin -> langsung tarif tetap tanpa mencoba RajaOngkir")
    void tanpaLokasiAsalLangsungTarifTetap() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn(null);
        when(ongkirRepository.findByProvinsi("DKI Jakarta")).thenReturn(Optional.empty());

        ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(1_000_000));

        verify(alamatOngkirResolver, never()).resolveDestinationId(any());
        verify(rajaOngkirClient, never()).hitungOngkir(any(), any(), anyInt(), anyList());
    }

    @Test
    @DisplayName("Subtotal mencapai minimal belanja -> ongkir nol terlepas dari sumber aslinya")
    void subtotalMencapaiMinimalJadiGratis() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn("501");
        when(alamatOngkirResolver.resolveDestinationId(alamat)).thenReturn(Optional.of("574"));
        when(settingService.getList("shipping.rajaongkir-kurir")).thenReturn(List.of("jne"));
        when(rajaOngkirClient.hitungOngkir("501", "574", 500, List.of("jne")))
                .thenReturn(Optional.of(List.of(tarif("jne", 20000, "2-3 day"))));
        when(settingService.getInt("shipping.gratis-ongkir-minimal", 0)).thenReturn(5_000_000);

        HasilOngkir hasil = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(5_000_000));

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_GRATIS_MINIMAL_BELANJA);
        assertThat(hasil.getTarif()).isEqualByComparingTo("0");
        assertThat(hasil.getKurirCode()).isEqualTo("jne");
        assertThat(hasil.getLayanan()).isEqualTo("REG");
        assertThat(hasil.getOpsi()).hasSize(1);
        assertThat(hasil.getOpsi().get(0).getTarif()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Subtotal belum mencapai minimal belanja -> ongkir asli tetap dipakai")
    void subtotalBelumMencapaiMinimalTetapBerbayar() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn(null);
        when(ongkirRepository.findByProvinsi("DKI Jakarta"))
                .thenReturn(Optional.of(Ongkir.builder()
                        .provinsi("DKI Jakarta")
                        .tarif(new BigDecimal("15000.00"))
                        .estimasiHari(2)
                        .build()));
        when(settingService.getInt("shipping.gratis-ongkir-minimal", 0)).thenReturn(5_000_000);

        HasilOngkir hasil = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(1_000_000));

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_TARIF_TETAP);
        assertThat(hasil.getTarif()).isEqualByComparingTo("15000.00");
        assertThat(hasil.getOpsi()).isEmpty();
    }

    @Test
    @DisplayName("Pilihan kurir+layanan yang cocok dengan hasil RajaOngkir dipakai apa adanya")
    void pilihanCocokDipakaiApaAdanya() {
        mockRajaOngkirDuaOpsi();

        HasilOngkir hasil = ongkirCalculationService.hitung(
                alamat, 500, BigDecimal.valueOf(1_000_000), "jne", "YES");

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_RAJAONGKIR);
        assertThat(hasil.getTarif()).isEqualByComparingTo("25000");
        assertThat(hasil.getEstimasiHari()).isEqualTo(1);
        assertThat(hasil.getKurirCode()).isEqualTo("jne");
        assertThat(hasil.getLayanan()).isEqualTo("YES");
        assertThat(hasil.getOpsi()).hasSize(2);
    }

    @Test
    @DisplayName("Pilihan tidak cocok (dipalsukan / quote basi) jatuh ke termurah tanpa exception")
    void pilihanTidakCocokJatuhKeTermurahTanpaException() {
        mockRajaOngkirDuaOpsi();

        HasilOngkir hasil = ongkirCalculationService.hitung(
                alamat, 500, BigDecimal.valueOf(1_000_000), "kurir-asalan", "XYZ");

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_RAJAONGKIR);
        assertThat(hasil.getTarif()).isEqualByComparingTo("12000");
        assertThat(hasil.getKurirCode()).isEqualTo("tiki");
        assertThat(hasil.getLayanan()).isEqualTo("REG");
    }

    @Test
    @DisplayName("Tidak ada pilihan kurir -> perilaku identik dengan hitung tanpa overload")
    void tanpaPilihanPerilakuSamaSepertiSebelumnya() {
        mockRajaOngkirDuaOpsi();

        HasilOngkir tanpaPilihan = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(1_000_000));
        HasilOngkir pilihanKosong = ongkirCalculationService.hitung(
                alamat, 500, BigDecimal.valueOf(1_000_000), null, null);

        assertThat(tanpaPilihan.getTarif()).isEqualByComparingTo(pilihanKosong.getTarif());
        assertThat(tanpaPilihan.getKurirCode()).isEqualTo(pilihanKosong.getKurirCode());
        assertThat(tanpaPilihan.getLayanan()).isEqualTo(pilihanKosong.getLayanan());
        assertThat(tanpaPilihan.getTarif()).isEqualByComparingTo("12000");
        assertThat(tanpaPilihan.getKurirCode()).isEqualTo("tiki");
    }

    @Test
    @DisplayName("hitungSemuaOpsi kosong kalau RajaOngkir tidak bisa dipakai")
    void hitungSemuaOpsiKosongKalauRajaOngkirTidakBisa() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn(null);

        assertThat(ongkirCalculationService.hitungSemuaOpsi(alamat, 500)).isEmpty();
    }

    @Test
    @DisplayName("Gratis minimal belanja tetap mengirim daftar kurir (tarif 0) supaya pembeli bisa memilih")
    void gratisMinimalTetapAdaPilihanKurirDariPengaturan() {
        when(settingService.getValue("shipping.origin-rajaongkir-id")).thenReturn(null);
        when(ongkirRepository.findByProvinsi("DKI Jakarta")).thenReturn(Optional.empty());
        when(settingService.getList("shipping.rajaongkir-kurir")).thenReturn(List.of("jne", "tiki", "jnt"));
        when(settingService.getInt("shipping.gratis-ongkir-minimal", 0)).thenReturn(5_000_000);

        HasilOngkir hasil = ongkirCalculationService.hitung(alamat, 500, BigDecimal.valueOf(20_000_000));

        assertThat(hasil.getSumber()).isEqualTo(OngkirCalculationService.SUMBER_GRATIS_MINIMAL_BELANJA);
        assertThat(hasil.getTarif()).isEqualByComparingTo("0");
        assertThat(hasil.getOpsi()).extracting("kurirCode").containsExactly("jne", "tiki", "jnt");
        assertThat(hasil.getOpsi()).allMatch(o -> o.getTarif().compareTo(BigDecimal.ZERO) == 0);
    }
}
