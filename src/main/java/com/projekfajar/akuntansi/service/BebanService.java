package com.projekfajar.akuntansi.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.dto.BebanRequest;
import com.projekfajar.akuntansi.dto.BebanResponse;
import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Beban;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.model.TipeAkun;
import com.projekfajar.akuntansi.repository.AkunRepository;
import com.projekfajar.akuntansi.repository.BebanRepository;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Biaya operasional: uang keluar yang langsung mengurangi laba.
 *
 * Akun tujuannya dibatasi bertipe BEBAN — tanpa batasan itu admin bisa saja
 * memilih akun kas atau pendapatan dan laporan laba rugi jadi tidak masuk akal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BebanService {

    public static final String AKUN_KAS = "1-100";

    private final BebanRepository bebanRepository;
    private final AkunRepository akunRepository;
    private final JurnalRepository jurnalRepository;
    private final JurnalService jurnalService;

    @Transactional(readOnly = true)
    public List<BebanResponse> daftar(LocalDate mulai, LocalDate sampai) {
        log.debug("Listing beban from {} to {}", mulai, sampai);
        List<BebanResponse> hasil = bebanRepository
                .findByTanggalBetweenOrderByTanggalDescIdDesc(mulai, sampai)
                .stream()
                .map(this::keResponse)
                .toList();
        log.debug("Found {} beban rows from {} to {}", hasil.size(), mulai, sampai);
        return hasil;
    }

    @Transactional
    public BebanResponse buat(BebanRequest request) {
        log.info("Creating beban: account={}, date={}, amount={}",
                request.getKodeAkun(), request.getTanggal(), request.getJumlah());
        Akun akun = akunRepository.findByKode(request.getKodeAkun())
                .orElseThrow(() -> {
                    log.warn("Beban rejected: account {} not found", request.getKodeAkun());
                    return new ResourceNotFoundException(
                            "Akun " + request.getKodeAkun() + " tidak ditemukan");
                });

        if (akun.getTipe() != TipeAkun.BEBAN) {
            log.warn("Beban rejected: account {} has type {}, expected BEBAN",
                    akun.getKode(), akun.getTipe());
            throw new BusinessException(
                    "Akun " + akun.getKode() + " (" + akun.getNama() + ") bukan akun beban");
        }

        LocalDate tanggal = request.getTanggal() != null ? request.getTanggal() : LocalDate.now();

        Beban beban = bebanRepository.save(Beban.builder()
                .tanggal(tanggal)
                .akun(akun)
                .keterangan(request.getKeterangan().trim())
                .jumlah(request.getJumlah())
                .createdAt(LocalDateTime.now())
                .build());

        jurnalService.catat(tanggal, beban.getKeterangan(), SumberJurnal.BEBAN, beban.getId(), List.of(
                Baris.debit(akun.getKode(), beban.getJumlah(), beban.getKeterangan()),
                Baris.kredit(AKUN_KAS, beban.getJumlah(), beban.getKeterangan())));

        log.info("Beban created: id={}, account={}, amount={}, date={}",
                beban.getId(), akun.getKode(), beban.getJumlah(), tanggal);

        return keResponse(beban);
    }

    @Transactional
    public BebanResponse batalkan(Long id, String alasan) {
        log.info("Cancelling beban id={}, reason={}", id, alasan);
        Beban beban = bebanRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Beban cancel rejected: id {} not found", id);
                    return new ResourceNotFoundException("Beban tidak ditemukan");
                });

        if (Boolean.TRUE.equals(beban.getDibatalkan())) {
            log.warn("Beban cancel rejected: id {} already cancelled", id);
            throw new BusinessException("Beban ini sudah dibatalkan");
        }

        List<Jurnal> jurnalAsal = jurnalRepository
                .findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.BEBAN, beban.getId());

        for (Jurnal jurnal : jurnalAsal) {
            jurnalService.catatBalik(jurnal, alasan != null ? alasan : "beban dibatalkan");
        }

        beban.setDibatalkan(true);
        beban.setDibatalkanAt(LocalDateTime.now());

        log.info("Beban cancelled: id={}, reversedJournals={}, reason={}",
                beban.getId(), jurnalAsal.size(), alasan);

        return keResponse(bebanRepository.save(beban));
    }

    private BebanResponse keResponse(Beban beban) {
        return BebanResponse.builder()
                .id(beban.getId())
                .tanggal(beban.getTanggal())
                .kodeAkun(beban.getAkun().getKode())
                .namaAkun(beban.getAkun().getNama())
                .keterangan(beban.getKeterangan())
                .jumlah(beban.getJumlah())
                .dibatalkan(Boolean.TRUE.equals(beban.getDibatalkan()))
                .createdAt(beban.getCreatedAt())
                .build();
    }
}
