package com.projekfajar.akuntansi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.model.TipeAkun;
import com.projekfajar.akuntansi.repository.AkunRepository;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;

/**
 * Aturan tunggal yang menjaga seluruh pembukuan: tidak ada jurnal yang boleh
 * tersimpan bila debit dan kreditnya tidak sama. Bila aturan ini bocor, neraca
 * tidak akan pernah seimbang dan penyebabnya sangat sulit ditelusuri belakangan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JurnalServiceTest {

    @Mock
    private JurnalRepository jurnalRepository;
    @Mock
    private AkunRepository akunRepository;

    @InjectMocks
    private JurnalService jurnalService;

    private static final String KAS = "1-100";
    private static final String PENDAPATAN = "4-100";

    @BeforeEach
    void setUp() {
        when(akunRepository.findByKode(KAS)).thenReturn(Optional.of(
                akun(1L, KAS, "Kas & Bank", TipeAkun.ASET, "DEBIT")));
        when(akunRepository.findByKode(PENDAPATAN)).thenReturn(Optional.of(
                akun(2L, PENDAPATAN, "Pendapatan Penjualan", TipeAkun.PENDAPATAN, "KREDIT")));
        when(jurnalRepository.save(any(Jurnal.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Akun akun(Long id, String kode, String nama, TipeAkun tipe, String saldoNormal) {
        return Akun.builder().id(id).kode(kode).nama(nama).tipe(tipe).saldoNormal(saldoNormal).build();
    }

    private Jurnal catat(List<Baris> baris) {
        return jurnalService.catat(LocalDate.of(2026, 8, 12), "Uji", SumberJurnal.MANUAL, 1L, baris);
    }

    @Test
    @DisplayName("Jurnal seimbang tersimpan dengan nomor berpola JRN-tanggal-XXXX")
    void jurnalSeimbangTersimpan() {
        Jurnal jurnal = catat(List.of(
                Baris.debit(KAS, new BigDecimal("150000"), "Terima uang"),
                Baris.kredit(PENDAPATAN, new BigDecimal("150000"), "Penjualan")));

        assertThat(jurnal.seimbang()).isTrue();
        assertThat(jurnal.getDetails()).hasSize(2);
        assertThat(jurnal.totalDebit()).isEqualByComparingTo("150000");
        assertThat(jurnal.getNomor()).matches("JRN-20260812-[0-9A-F]{4}");
        verify(jurnalRepository).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Jurnal tidak seimbang ditolak dan tidak tersimpan")
    void jurnalTidakSeimbangDitolak() {
        assertThatThrownBy(() -> catat(List.of(
                Baris.debit(KAS, new BigDecimal("150000"), "Terima uang"),
                Baris.kredit(PENDAPATAN, new BigDecimal("100000"), "Penjualan"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tidak seimbang");

        verify(jurnalRepository, never()).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Satu baris tidak boleh diisi debit dan kredit sekaligus")
    void barisGandaDitolak() {
        assertThatThrownBy(() -> catat(List.of(
                new Baris(KAS, new BigDecimal("150000"), new BigDecimal("150000"), "Rancu"),
                Baris.kredit(PENDAPATAN, new BigDecimal("150000"), "Penjualan"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hanya boleh diisi debit atau kredit");

        verify(jurnalRepository, never()).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Nilai negatif ditolak, karena pembalikan harus lewat sisi lawan")
    void nilaiNegatifDitolak() {
        assertThatThrownBy(() -> catat(List.of(
                Baris.debit(KAS, new BigDecimal("-150000"), "Salah tanda"),
                Baris.kredit(PENDAPATAN, new BigDecimal("-150000"), "Salah tanda"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tidak boleh negatif");

        verify(jurnalRepository, never()).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Jurnal berisi kurang dari dua baris ditolak")
    void jurnalSatuBarisDitolak() {
        assertThatThrownBy(() -> catat(List.of(
                Baris.debit(KAS, new BigDecimal("150000"), "Sendirian"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("minimal dua baris");

        verify(jurnalRepository, never()).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Baris bernilai nol dibuang, dan jurnal yang tersisa satu baris ditolak")
    void barisNolDibuang() {
        assertThatThrownBy(() -> catat(List.of(
                Baris.debit(KAS, new BigDecimal("150000"), "Isi"),
                Baris.kredit(PENDAPATAN, BigDecimal.ZERO, "Kosong"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("minimal dua baris berisi nilai");

        verify(jurnalRepository, never()).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Akun yang tidak ada di bagan akun menggagalkan pencatatan")
    void akunTidakDikenalDitolak() {
        when(akunRepository.findByKode("9-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catat(List.of(
                Baris.debit("9-999", new BigDecimal("150000"), "Akun karangan"),
                Baris.kredit(PENDAPATAN, new BigDecimal("150000"), "Penjualan"))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jurnalRepository, never()).save(any(Jurnal.class));
    }

    @Test
    @DisplayName("Jurnal balik menukar sisi debit dan kredit, bukan menghapus jurnal asal")
    void jurnalBalikMenukarSisi() {
        Jurnal asal = catat(List.of(
                Baris.debit(KAS, new BigDecimal("150000"), "Terima uang"),
                Baris.kredit(PENDAPATAN, new BigDecimal("150000"), "Penjualan")));
        asal.setId(7L);

        Jurnal balik = jurnalService.catatBalik(asal, "salah input");

        assertThat(balik.seimbang()).isTrue();
        assertThat(balik.getSumber()).isEqualTo(SumberJurnal.MANUAL);
        assertThat(balik.getReferensiId()).isEqualTo(7L);
        assertThat(balik.getKeterangan()).contains(asal.getNomor()).contains("salah input");

        // Kas yang tadi didebit sekarang dikredit
        assertThat(balik.getDetails().stream()
                .filter(d -> d.getAkun().getKode().equals(KAS))
                .findFirst().orElseThrow().getKredit())
                .isEqualByComparingTo("150000");
    }
}
