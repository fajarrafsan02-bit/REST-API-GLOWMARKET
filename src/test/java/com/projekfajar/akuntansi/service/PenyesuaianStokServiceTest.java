package com.projekfajar.akuntansi.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.projekfajar.produk.model.Produk;

/**
 * Stok yang diubah admin harus punya lawan jurnal, kalau tidak saldo Persediaan
 * di neraca akan menyimpang dari barang yang benar-benar ada di toko.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PenyesuaianStokServiceTest {

    @Mock
    private JurnalService jurnalService;

    @Mock
    private JurnalRepository jurnalRepository;

    @InjectMocks
    private PenyesuaianStokService penyesuaianStokService;

    private Produk produk;

    @BeforeEach
    void setUp() {
        when(jurnalRepository.existsBySumber(SumberJurnal.SALDO_AWAL)).thenReturn(true);

        produk = Produk.builder()
                .id(10L)
                .nama("Cincin Emas")
                .harga(new BigDecimal("1000000.00"))
                .hargaModal(new BigDecimal("700000.00"))
                .stock(5)
                .karatEmas(18)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Baris> tangkapJurnal() {
        ArgumentCaptor<List<Baris>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurnalService).catat(any(LocalDate.class), anyString(),
                eq(SumberJurnal.PENYESUAIAN), eq(10L), captor.capture());
        return captor.getValue();
    }

    private BigDecimal nilai(List<Baris> baris, String kodeAkun, boolean sisiDebit) {
        return baris.stream()
                .filter(b -> b.kodeAkun().equals(kodeAkun))
                .map(b -> sisiDebit ? b.debit() : b.kredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("Stok bertambah dicatat sebagai tambahan persediaan dan modal, bukan keuntungan")
    void stokBertambahMasukModal() {
        penyesuaianStokService.catat(produk, 5, 8, "koreksi");

        List<Baris> jurnal = tangkapJurnal();

        // 3 unit x 700.000
        assertThat(nilai(jurnal, PenyesuaianStokService.AKUN_PERSEDIAAN, true))
                .isEqualByComparingTo("2100000");
        assertThat(nilai(jurnal, PenyesuaianStokService.AKUN_MODAL, false))
                .isEqualByComparingTo("2100000");

        // Tidak boleh menyentuh akun pendapatan mana pun
        assertThat(jurnal).noneMatch(b -> b.kodeAkun().startsWith("4-"));
    }

    @Test
    @DisplayName("Stok berkurang dicatat sebagai kerugian selisih persediaan")
    void stokBerkurangJadiKerugian() {
        penyesuaianStokService.catat(produk, 5, 3, "barang rusak");

        List<Baris> jurnal = tangkapJurnal();

        // 2 unit x 700.000
        assertThat(nilai(jurnal, PenyesuaianStokService.AKUN_SELISIH, true))
                .isEqualByComparingTo("1400000");
        assertThat(nilai(jurnal, PenyesuaianStokService.AKUN_PERSEDIAAN, false))
                .isEqualByComparingTo("1400000");
    }

    @Test
    @DisplayName("Stok tidak berubah tidak menghasilkan jurnal")
    void stokTetapTidakDijurnal() {
        penyesuaianStokService.catat(produk, 5, 5, "simpan tanpa ubah stok");

        verify(jurnalService, never()).catat(any(), anyString(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Produk tanpa harga modal dilewati, bukan dijurnal senilai nol")
    void tanpaHargaModalDilewati() {
        produk.setHargaModal(BigDecimal.ZERO);

        penyesuaianStokService.catat(produk, 5, 9, "koreksi");

        verify(jurnalService, never()).catat(any(), anyString(), any(), any(), anyList());
    }

    @Test
    @DisplayName("Sebelum saldo awal, stok tidak dijurnal supaya persediaan tidak tercatat dua kali")
    void sebelumSaldoAwalTidakDijurnal() {
        when(jurnalRepository.existsBySumber(SumberJurnal.SALDO_AWAL)).thenReturn(false);

        penyesuaianStokService.catat(produk, 0, 5, "stok awal produk baru");

        verify(jurnalService, never()).catat(any(), anyString(), any(), any(), anyList());
    }
}
