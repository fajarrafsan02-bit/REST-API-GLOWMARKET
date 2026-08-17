package com.projekfajar.alamat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.alamat.repository.AlamatRepository;
import com.projekfajar.ongkir.service.RajaOngkirClient;

/**
 * Pencarian lokasi adalah bagian yang paling memakan kuota harian RajaOngkir,
 * jadi harus benar-benar hanya terjadi sekali per alamat -- bukan setiap kali
 * ongkir dihitung.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlamatOngkirResolverTest {

    @Mock
    private RajaOngkirClient rajaOngkirClient;

    @Mock
    private AlamatRepository alamatRepository;

    @InjectMocks
    private AlamatOngkirResolver alamatOngkirResolver;

    private Alamat alamatBaru() {
        return Alamat.builder()
                .id(1L)
                .provinsi("DKI Jakarta")
                .kota("Jakarta Selatan")
                .kecamatan("Kebayoran Baru")
                .kelurahan("Senayan")
                .kodePos("12190")
                .build();
    }

    @Test
    @DisplayName("Alamat yang sudah punya ID RajaOngkir tidak memanggil pencarian sama sekali")
    void alamatSudahTerCacheTidakMencari() {
        Alamat alamat = alamatBaru();
        alamat.setRajaongkirDestinationId("999");

        Optional<String> hasil = alamatOngkirResolver.resolveDestinationId(alamat);

        assertThat(hasil).contains("999");
        verifyNoInteractions(rajaOngkirClient);
        verify(alamatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alamat baru dicari sekali dan hasilnya disimpan balik ke Alamat")
    void alamatBaruDicariDanDisimpan() {
        Alamat alamat = alamatBaru();
        when(rajaOngkirClient.cariLokasi(anyString())).thenReturn(List.of(
                Map.of("id", 574, "province_name", "DKI Jakarta", "subdistrict_name", "Kebayoran Baru")));

        Optional<String> hasil = alamatOngkirResolver.resolveDestinationId(alamat);

        assertThat(hasil).contains("574");
        assertThat(alamat.getRajaongkirDestinationId()).isEqualTo("574");
        verify(alamatRepository).save(alamat);
    }

    @Test
    @DisplayName("Pencarian tidak menemukan hasil -> kosong dan tidak disimpan, supaya bisa dicoba lagi nanti")
    void pencarianKosongTidakDisimpan() {
        Alamat alamat = alamatBaru();
        when(rajaOngkirClient.cariLokasi(anyString())).thenReturn(List.of());

        Optional<String> hasil = alamatOngkirResolver.resolveDestinationId(alamat);

        assertThat(hasil).isEmpty();
        verify(alamatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Beberapa hasil pencarian -> dipilih yang provinsinya cocok, bukan sekadar hasil pertama")
    void memilihHasilYangProvinsinyaCocok() {
        Alamat alamat = alamatBaru();
        when(rajaOngkirClient.cariLokasi(anyString())).thenReturn(List.of(
                Map.of("id", 100, "province_name", "Jawa Barat"),
                Map.of("id", 574, "province_name", "DKI Jakarta")));

        Optional<String> hasil = alamatOngkirResolver.resolveDestinationId(alamat);

        assertThat(hasil).contains("574");
    }
}
