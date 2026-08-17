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

import com.projekfajar.akuntansi.dto.BebanRequest;
import com.projekfajar.akuntansi.dto.BebanResponse;
import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Beban;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.model.TipeAkun;
import com.projekfajar.akuntansi.repository.AkunRepository;
import com.projekfajar.akuntansi.repository.BebanRepository;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BebanServiceTest {

    @Mock
    private BebanRepository bebanRepository;
    @Mock
    private AkunRepository akunRepository;
    @Mock
    private JurnalRepository jurnalRepository;
    @Mock
    private JurnalService jurnalService;

    @InjectMocks
    private BebanService bebanService;

    private static final String AKUN_SEWA = "6-200";

    @BeforeEach
    void setUp() {
        when(akunRepository.findByKode(AKUN_SEWA)).thenReturn(Optional.of(Akun.builder()
                .id(9L).kode(AKUN_SEWA).nama("Beban Sewa")
                .tipe(TipeAkun.BEBAN).saldoNormal("DEBIT").build()));

        when(bebanRepository.save(any(Beban.class))).thenAnswer(inv -> {
            Beban b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(77L);
            }
            return b;
        });
    }

    private BebanRequest permintaan(String kodeAkun, String jumlah) {
        return BebanRequest.builder()
                .tanggal(LocalDate.of(2026, 8, 12))
                .kodeAkun(kodeAkun)
                .keterangan("Sewa toko Agustus")
                .jumlah(new BigDecimal(jumlah))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Baris> tangkapJurnal() {
        ArgumentCaptor<List<Baris>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurnalService).catat(any(LocalDate.class), anyString(),
                eq(SumberJurnal.BEBAN), eq(77L), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Beban dicatat sebagai debit akun beban dan kredit kas")
    void bebanMendebitAkunBebanDanMengkreditKas() {
        BebanResponse hasil = bebanService.buat(permintaan(AKUN_SEWA, "2500000"));

        assertThat(hasil.getKodeAkun()).isEqualTo(AKUN_SEWA);
        assertThat(hasil.getJumlah()).isEqualByComparingTo("2500000");

        List<Baris> jurnal = tangkapJurnal();
        assertThat(jurnal).hasSize(2);

        Baris debit = jurnal.stream().filter(b -> b.kodeAkun().equals(AKUN_SEWA))
                .findFirst().orElseThrow();
        Baris kredit = jurnal.stream().filter(b -> b.kodeAkun().equals(BebanService.AKUN_KAS))
                .findFirst().orElseThrow();

        assertThat(debit.debit()).isEqualByComparingTo("2500000");
        assertThat(kredit.kredit()).isEqualByComparingTo("2500000");
    }

    @Test
    @DisplayName("Akun bukan bertipe BEBAN ditolak, agar laba rugi tidak kacau")
    void akunNonBebanDitolak() {
        when(akunRepository.findByKode("1-100")).thenReturn(Optional.of(Akun.builder()
                .id(1L).kode("1-100").nama("Kas & Bank")
                .tipe(TipeAkun.ASET).saldoNormal("DEBIT").build()));

        assertThatThrownBy(() -> bebanService.buat(permintaan("1-100", "2500000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bukan akun beban");

        verify(jurnalService, never()).catat(any(), anyString(), any(), any(), anyList());
        verify(bebanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pembatalan beban membuat jurnal balik dan tidak menghapus catatan")
    void pembatalanMembuatJurnalBalik() {
        Beban beban = Beban.builder()
                .id(77L)
                .tanggal(LocalDate.of(2026, 8, 12))
                .akun(Akun.builder().id(9L).kode(AKUN_SEWA).nama("Beban Sewa")
                        .tipe(TipeAkun.BEBAN).saldoNormal("DEBIT").build())
                .keterangan("Sewa toko Agustus")
                .jumlah(new BigDecimal("2500000"))
                .build();

        when(bebanRepository.findById(77L)).thenReturn(Optional.of(beban));
        when(jurnalRepository.findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.BEBAN, 77L))
                .thenReturn(List.of());

        BebanResponse hasil = bebanService.batalkan(77L, "dobel catat");

        assertThat(hasil.isDibatalkan()).isTrue();
        verify(bebanRepository, never()).delete(any());
    }
}
