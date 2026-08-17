package com.projekfajar.akuntansi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.model.PesananItem;
import com.projekfajar.produk.model.Produk;

/**
 * Menguji isi jurnal penjualan: nilai yang masuk ke tiap akun, dan yang paling
 * penting — HPP dihitung dari harga modal yang di-snapshot ke item pesanan,
 * bukan dari harga modal produk saat laporan dibuka.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostingPenjualanServiceTest {

    @Mock
    private JurnalService jurnalService;
    @Mock
    private JurnalRepository jurnalRepository;

    @InjectMocks
    private PostingPenjualanService postingPenjualanService;

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

        when(jurnalRepository.existsBySumberAndReferensiId(any(), any())).thenReturn(false);
    }

    private Pesanan pesanan(BigDecimal total, BigDecimal ongkir, BigDecimal modalSnapshot, int qty) {
        Pesanan pesanan = Pesanan.builder()
                .id(50L)
                .nomorPesanan("ORD-TEST")
                .totalHarga(total)
                .ongkir(ongkir)
                .status(OrderStatus.DIKEMAS)
                .createdAt(LocalDateTime.of(2026, 8, 12, 10, 0))
                .build();

        pesanan.setItems(List.of(PesananItem.builder()
                .id(1L)
                .pesanan(pesanan)
                .produk(produk)
                .namaProduk(produk.getNama())
                .quantity(qty)
                .hargaSatuan(produk.getHarga())
                .hargaModal(modalSnapshot)
                .subtotal(produk.getHarga().multiply(BigDecimal.valueOf(qty)))
                .build()));

        return pesanan;
    }

    @SuppressWarnings("unchecked")
    private List<List<Baris>> tangkapJurnal(int jumlahJurnal) {
        ArgumentCaptor<List<Baris>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurnalService, times(jumlahJurnal)).catat(
                any(LocalDate.class), anyString(), eq(SumberJurnal.PENJUALAN), eq(50L), captor.capture());
        return captor.getAllValues();
    }

    private BigDecimal nilai(List<Baris> baris, String kodeAkun, boolean sisiDebit) {
        return baris.stream()
                .filter(b -> b.kodeAkun().equals(kodeAkun))
                .map(b -> sisiDebit ? b.debit() : b.kredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal total(List<Baris> baris, boolean sisiDebit) {
        return baris.stream()
                .map(b -> sisiDebit ? b.debit() : b.kredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("Penjualan lunas menghasilkan dua jurnal seimbang: penjualan dan HPP")
    void penjualanMenghasilkanDuaJurnal() {
        // 2 unit x 1.000.000 + ongkir 50.000
        postingPenjualanService.catatPenjualan(
                pesanan(new BigDecimal("2050000.00"), new BigDecimal("50000.00"),
                        new BigDecimal("700000.00"), 2));

        List<List<Baris>> jurnal = tangkapJurnal(2);

        List<Baris> penjualan = jurnal.get(0);
        assertThat(nilai(penjualan, PostingPenjualanService.AKUN_KAS, true))
                .isEqualByComparingTo("2050000");
        assertThat(nilai(penjualan, PostingPenjualanService.AKUN_PENDAPATAN, false))
                .isEqualByComparingTo("2000000");
        assertThat(nilai(penjualan, PostingPenjualanService.AKUN_PENDAPATAN_ONGKIR, false))
                .isEqualByComparingTo("50000");
        assertThat(total(penjualan, true)).isEqualByComparingTo(total(penjualan, false));

        // HPP = 700.000 x 2
        List<Baris> hpp = jurnal.get(1);
        assertThat(nilai(hpp, PostingPenjualanService.AKUN_HPP, true))
                .isEqualByComparingTo("1400000");
        assertThat(nilai(hpp, PostingPenjualanService.AKUN_PERSEDIAAN, false))
                .isEqualByComparingTo("1400000");
        assertThat(total(hpp, true)).isEqualByComparingTo(total(hpp, false));
    }

    @Test
    @DisplayName("HPP memakai harga modal snapshot, bukan harga modal produk saat ini")
    void hppMemakaiSnapshotBukanHargaModalTerbaru() {
        // Modal saat barang terjual 600.000; setelahnya pemasok menaikkan harga
        Pesanan pesanan = pesanan(new BigDecimal("2000000.00"), BigDecimal.ZERO,
                new BigDecimal("600000.00"), 2);
        produk.setHargaModal(new BigDecimal("900000.00"));

        postingPenjualanService.catatPenjualan(pesanan);

        List<Baris> hpp = tangkapJurnal(2).get(1);

        // 600.000 x 2, bukan 900.000 x 2
        assertThat(nilai(hpp, PostingPenjualanService.AKUN_HPP, true))
                .isEqualByComparingTo("1200000");
    }

    @Test
    @DisplayName("Tanpa ongkir, jurnal penjualan tidak memuat baris pendapatan ongkir")
    void tanpaOngkirTidakAdaBarisOngkir() {
        postingPenjualanService.catatPenjualan(
                pesanan(new BigDecimal("2000000.00"), BigDecimal.ZERO,
                        new BigDecimal("700000.00"), 2));

        List<Baris> penjualan = tangkapJurnal(2).get(0);

        assertThat(penjualan).noneMatch(
                b -> b.kodeAkun().equals(PostingPenjualanService.AKUN_PENDAPATAN_ONGKIR));
        assertThat(nilai(penjualan, PostingPenjualanService.AKUN_PENDAPATAN, false))
                .isEqualByComparingTo("2000000");
    }

    @Test
    @DisplayName("Produk yang harga modalnya belum diisi: penjualan tetap dijurnal, HPP dilewati")
    void modalNolMelewatiJurnalHpp() {
        postingPenjualanService.catatPenjualan(
                pesanan(new BigDecimal("2000000.00"), BigDecimal.ZERO, BigDecimal.ZERO, 2));

        // Hanya jurnal penjualan yang terbentuk — laba salah lebih berbahaya daripada laba kosong
        List<Baris> penjualan = tangkapJurnal(1).get(0);
        assertThat(nilai(penjualan, PostingPenjualanService.AKUN_KAS, true))
                .isEqualByComparingTo("2000000");
    }

    @Test
    @DisplayName("Webhook yang datang dua kali tidak menjurnal penjualan yang sama")
    void webhookGandaTidakMenjurnalUlang() {
        when(jurnalRepository.existsBySumberAndReferensiId(SumberJurnal.PENJUALAN, 50L))
                .thenReturn(true);

        postingPenjualanService.catatPenjualan(
                pesanan(new BigDecimal("2000000.00"), BigDecimal.ZERO,
                        new BigDecimal("700000.00"), 2));

        verify(jurnalService, never()).catat(any(), anyString(), any(), any(), anyList());
    }
}
