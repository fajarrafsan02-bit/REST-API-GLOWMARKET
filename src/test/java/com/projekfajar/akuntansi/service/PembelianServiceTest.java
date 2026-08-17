package com.projekfajar.akuntansi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.projekfajar.akuntansi.dto.PembelianRequest;
import com.projekfajar.akuntansi.dto.PembelianResponse;
import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.JurnalDetail;
import com.projekfajar.akuntansi.model.MetodePembelian;
import com.projekfajar.akuntansi.model.Pembelian;
import com.projekfajar.akuntansi.model.PembelianItem;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.model.TipeAkun;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.repository.PembelianRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;

/**
 * Pembelian menyentuh dua hal sekaligus — stok dan pembukuan — jadi yang diuji
 * adalah keduanya bergerak bersama, termasuk saat pembelian dibatalkan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PembelianServiceTest {

    @Mock
    private PembelianRepository pembelianRepository;
    @Mock
    private ProdukRepository produkRepository;
    @Mock
    private JurnalRepository jurnalRepository;
    @Mock
    private JurnalService jurnalService;

    @InjectMocks
    private PembelianService pembelianService;

    private Produk produk;

    @BeforeEach
    void setUp() {
        produk = Produk.builder()
                .id(10L)
                .nama("Cincin Emas")
                .harga(new BigDecimal("1000000.00"))
                .hargaModal(new BigDecimal("700000.00"))
                .stock(5)
                .karatEmas(18)
                .build();

        when(produkRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(produk));
        when(produkRepository.save(any(Produk.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pembelianRepository.save(any(Pembelian.class))).thenAnswer(inv -> {
            Pembelian p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(99L);
            }
            return p;
        });
        when(jurnalRepository.findBySumberAndReferensiIdOrderByIdAsc(any(), any()))
                .thenReturn(List.of());
    }

    private PembelianRequest permintaan(int qty, String hargaBeli) {
        return PembelianRequest.builder()
                .tanggal(LocalDate.of(2026, 8, 12))
                .pemasok("Toko Emas Sinar")
                .items(List.of(PembelianRequest.Item.builder()
                        .produkId(10L)
                        .qty(qty)
                        .hargaBeli(new BigDecimal(hargaBeli))
                        .build()))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Baris> tangkapJurnal() {
        ArgumentCaptor<List<Baris>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurnalService).catat(any(LocalDate.class), anyString(),
                eq(SumberJurnal.PEMBELIAN), eq(99L), captor.capture());
        return captor.getValue();
    }

    private BigDecimal nilai(List<Baris> baris, String kodeAkun, boolean sisiDebit) {
        return baris.stream()
                .filter(b -> b.kodeAkun().equals(kodeAkun))
                .map(b -> sisiDebit ? b.debit() : b.kredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("Pembelian menambah stok, memperbarui harga modal, dan menjurnal seimbang")
    void pembelianMenambahStokDanMenjurnal() {
        PembelianResponse hasil = pembelianService.buat(permintaan(3, "800000"));

        // 5 + 3
        assertThat(produk.getStock()).isEqualTo(8);
        // Harga modal mengikuti harga beli terakhir
        assertThat(produk.getHargaModal()).isEqualByComparingTo("800000");
        assertThat(hasil.getTotal()).isEqualByComparingTo("2400000");
        assertThat(hasil.getMetode()).isEqualTo(MetodePembelian.TUNAI);
        assertThat(hasil.isDilunasi()).isTrue();
        assertThat(hasil.getNomor()).matches("BLI-20260812-[0-9A-F]{4}");

        List<Baris> jurnal = tangkapJurnal();
        assertThat(nilai(jurnal, PembelianService.AKUN_PERSEDIAAN, true))
                .isEqualByComparingTo("2400000");
        assertThat(nilai(jurnal, PembelianService.AKUN_KAS, false))
                .isEqualByComparingTo("2400000");
    }

    @Test
    @DisplayName("Pembelian bernilai nol ditolak dan tidak dijurnal")
    void pembelianNolDitolak() {
        assertThatThrownBy(() -> pembelianService.buat(permintaan(2, "0")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("lebih dari nol");

        verify(jurnalService, never()).catat(any(), anyString(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Pembatalan mengembalikan stok dan membuat jurnal balik, bukan menghapus jurnal")
    void pembatalanMengembalikanStokDanMembalikJurnal() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        produk.setStock(8); // stok setelah pembelian
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        Jurnal asal = jurnalPembelian();
        when(jurnalRepository.findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.PEMBELIAN, 99L))
                .thenReturn(List.of(asal));

        PembelianResponse hasil = pembelianService.batalkan(99L, "salah input");

        assertThat(produk.getStock()).isEqualTo(5);
        assertThat(hasil.isDibatalkan()).isTrue();
        verify(jurnalService).catatBalik(asal, "salah input");
        verify(jurnalRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Pembelian yang stoknya sudah terjual tidak bisa dibatalkan")
    void pembatalanDitolakBilaStokSudahTerjual() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        produk.setStock(1); // dua unit sudah terjual
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        assertThatThrownBy(() -> pembelianService.batalkan(99L, "salah input"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sudah terjual");

        assertThat(produk.getStock()).isEqualTo(1);
        verify(jurnalService, never()).catatBalik(any(), anyString());
    }

    @Test
    @DisplayName("Pembelian yang sudah dibatalkan tidak bisa dibatalkan dua kali")
    void pembatalanGandaDitolak() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        pembelian.setDibatalkan(true);
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        assertThatThrownBy(() -> pembelianService.batalkan(99L, "salah input"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sudah dibatalkan");

        verify(jurnalService, never()).catatBalik(any(), anyString());
    }

    @Test
    @DisplayName("Pembelian kredit menambah stok tanpa mengurangi kas, dan mencatat utang usaha")
    void pembelianKreditMencatatUtang() {
        PembelianRequest request = permintaan(3, "800000");
        request.setMetode(MetodePembelian.KREDIT);

        PembelianResponse hasil = pembelianService.buat(request);

        assertThat(produk.getStock()).isEqualTo(8);
        assertThat(hasil.getMetode()).isEqualTo(MetodePembelian.KREDIT);
        assertThat(hasil.isDilunasi()).isFalse();

        List<Baris> jurnal = tangkapJurnal();
        assertThat(nilai(jurnal, PembelianService.AKUN_PERSEDIAAN, true))
                .isEqualByComparingTo("2400000");
        assertThat(nilai(jurnal, PembelianService.AKUN_UTANG, false))
                .isEqualByComparingTo("2400000");
        assertThat(jurnal).noneMatch(b -> b.kodeAkun().equals(PembelianService.AKUN_KAS));
    }

    @Test
    @DisplayName("Pelunasan utang mengurangi kas dan utang, stok tidak berubah")
    @SuppressWarnings("unchecked")
    void pelunasanMengurangiKasDanUtang() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        pembelian.setMetode(MetodePembelian.KREDIT);
        pembelian.setDilunasi(false);
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        int stokSebelum = produk.getStock();
        PembelianResponse hasil = pembelianService.lunasi(99L);

        assertThat(hasil.isDilunasi()).isTrue();
        assertThat(produk.getStock()).isEqualTo(stokSebelum);

        ArgumentCaptor<List<Baris>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurnalService).catat(any(LocalDate.class), anyString(),
                eq(SumberJurnal.PELUNASAN), eq(99L), captor.capture());

        List<Baris> jurnal = captor.getValue();
        assertThat(nilai(jurnal, PembelianService.AKUN_UTANG, true))
                .isEqualByComparingTo("2400000");
        assertThat(nilai(jurnal, PembelianService.AKUN_KAS, false))
                .isEqualByComparingTo("2400000");
    }

    @Test
    @DisplayName("Pembelian tunai tidak bisa dilunasi ulang")
    void pelunasanTunaiDitolak() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        assertThatThrownBy(() -> pembelianService.lunasi(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tunai");

        verify(jurnalService, never()).catat(any(), anyString(), eq(SumberJurnal.PELUNASAN), any(), anyList());
    }

    @Test
    @DisplayName("Utang yang sudah lunas tidak bisa dilunasi dua kali")
    void pelunasanGandaDitolak() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        pembelian.setMetode(MetodePembelian.KREDIT);
        pembelian.setDilunasi(true);
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        assertThatThrownBy(() -> pembelianService.lunasi(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sudah dilunasi");
    }

    @Test
    @DisplayName("Pembatalan pembelian kredit yang sudah dilunasi membalik jurnal pembelian dan pelunasan")
    void pembatalanSetelahLunasMembalikKeduaJurnal() {
        Pembelian pembelian = pembelianTersimpan(3, "800000");
        pembelian.setMetode(MetodePembelian.KREDIT);
        pembelian.setDilunasi(true);
        produk.setStock(8);
        when(pembelianRepository.findWithItemsById(99L)).thenReturn(Optional.of(pembelian));

        Jurnal asal = jurnalPembelian();
        Jurnal pelunasan = Jurnal.builder()
                .id(501L)
                .nomor("JRN-20260813-BBBB")
                .tanggal(LocalDate.of(2026, 8, 13))
                .keterangan("Pelunasan utang BLI-20260812-ABCD")
                .sumber(SumberJurnal.PELUNASAN)
                .referensiId(99L)
                .build();

        when(jurnalRepository.findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.PEMBELIAN, 99L))
                .thenReturn(List.of(asal));
        when(jurnalRepository.findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.PELUNASAN, 99L))
                .thenReturn(List.of(pelunasan));

        pembelianService.batalkan(99L, "salah input");

        verify(jurnalService).catatBalik(asal, "salah input");
        verify(jurnalService).catatBalik(pelunasan, "salah input");
    }

    private Pembelian pembelianTersimpan(int qty, String hargaBeli) {
        BigDecimal harga = new BigDecimal(hargaBeli);
        BigDecimal subtotal = harga.multiply(BigDecimal.valueOf(qty));

        Pembelian pembelian = Pembelian.builder()
                .id(99L)
                .nomor("BLI-20260812-ABCD")
                .tanggal(LocalDate.of(2026, 8, 12))
                .pemasok("Toko Emas Sinar")
                .total(subtotal)
                .build();

        pembelian.setItems(List.of(PembelianItem.builder()
                .id(1L)
                .pembelian(pembelian)
                .produk(produk)
                .namaProduk(produk.getNama())
                .qty(qty)
                .hargaBeli(harga)
                .subtotal(subtotal)
                .build()));

        return pembelian;
    }

    private Jurnal jurnalPembelian() {
        Akun persediaan = Akun.builder().id(2L).kode(PembelianService.AKUN_PERSEDIAAN)
                .nama("Persediaan Barang").tipe(TipeAkun.ASET).saldoNormal("DEBIT").build();
        Akun kas = Akun.builder().id(1L).kode(PembelianService.AKUN_KAS)
                .nama("Kas & Bank").tipe(TipeAkun.ASET).saldoNormal("DEBIT").build();

        Jurnal jurnal = Jurnal.builder()
                .id(500L)
                .nomor("JRN-20260812-AAAA")
                .tanggal(LocalDate.of(2026, 8, 12))
                .keterangan("Pembelian BLI-20260812-ABCD")
                .sumber(SumberJurnal.PEMBELIAN)
                .referensiId(99L)
                .build();

        jurnal.setDetails(List.of(
                JurnalDetail.builder().jurnal(jurnal).akun(persediaan)
                        .debit(new BigDecimal("2400000")).kredit(BigDecimal.ZERO).build(),
                JurnalDetail.builder().jurnal(jurnal).akun(kas)
                        .debit(BigDecimal.ZERO).kredit(new BigDecimal("2400000")).build()));

        return jurnal;
    }
}
