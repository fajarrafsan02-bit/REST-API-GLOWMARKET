package com.projekfajar.akuntansi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.JurnalDetail;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.AkunRepository;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Satu-satunya pintu untuk membuat jurnal.
 *
 * Semua pencatatan — penjualan, pembelian, beban, saldo awal, koreksi manual —
 * lewat sini supaya aturan "debit harus sama dengan kredit" tidak bisa dilewati
 * oleh satu pun jalur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JurnalService {

    private final JurnalRepository jurnalRepository;
    private final AkunRepository akunRepository;

    /** Satu baris jurnal yang belum tersimpan. */
    public record Baris(String kodeAkun, BigDecimal debit, BigDecimal kredit, String keterangan) {

        public static Baris debit(String kodeAkun, BigDecimal jumlah, String keterangan) {
            return new Baris(kodeAkun, jumlah, BigDecimal.ZERO, keterangan);
        }

        public static Baris kredit(String kodeAkun, BigDecimal jumlah, String keterangan) {
            return new Baris(kodeAkun, BigDecimal.ZERO, jumlah, keterangan);
        }
    }

    @Transactional
    public Jurnal catat(LocalDate tanggal, String keterangan, SumberJurnal sumber,
            Long referensiId, List<Baris> baris) {

        if (baris == null || baris.size() < 2) {
            throw new BusinessException("Jurnal harus punya minimal dua baris");
        }

        Jurnal jurnal = Jurnal.builder()
                .nomor(buatNomor(tanggal))
                .tanggal(tanggal)
                .keterangan(keterangan)
                .sumber(sumber)
                .referensiId(referensiId)
                .createdAt(LocalDateTime.now())
                .build();

        List<JurnalDetail> details = new ArrayList<>();

        for (Baris b : baris) {
            Akun akun = akunRepository.findByKode(b.kodeAkun())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Akun " + b.kodeAkun() + " tidak ditemukan"));

            BigDecimal debit = nolBilaKosong(b.debit());
            BigDecimal kredit = nolBilaKosong(b.kredit());

            if (debit.signum() < 0 || kredit.signum() < 0) {
                throw new BusinessException("Nilai debit/kredit tidak boleh negatif");
            }

            if (debit.signum() > 0 && kredit.signum() > 0) {
                throw new BusinessException(
                        "Satu baris jurnal hanya boleh diisi debit atau kredit, tidak keduanya");
            }

            // Baris kosong tidak perlu disimpan
            if (debit.signum() == 0 && kredit.signum() == 0) {
                continue;
            }

            details.add(JurnalDetail.builder()
                    .jurnal(jurnal)
                    .akun(akun)
                    .debit(debit)
                    .kredit(kredit)
                    .keterangan(b.keterangan())
                    .build());
        }

        jurnal.setDetails(details);

        if (details.size() < 2) {
            throw new BusinessException("Jurnal harus punya minimal dua baris berisi nilai");
        }

        if (!jurnal.seimbang()) {
            throw new BusinessException(String.format(
                    "Jurnal tidak seimbang: debit %s, kredit %s",
                    jurnal.totalDebit(), jurnal.totalKredit()));
        }

        Jurnal tersimpan = jurnalRepository.save(jurnal);
        log.info("Jurnal {} tercatat ({}): {}", tersimpan.getNomor(), sumber, keterangan);

        return tersimpan;
    }

    /**
     * Jurnal balik untuk membatalkan jurnal yang sudah tercatat.
     * Pembukuan tidak boleh menghapus jejak, jadi koreksi dicatat sebagai entri baru.
     */
    @Transactional
    public Jurnal catatBalik(Jurnal asal, String alasan) {
        List<Baris> baris = asal.getDetails().stream()
                .map(d -> new Baris(
                        d.getAkun().getKode(),
                        d.getKredit(),
                        d.getDebit(),
                        "Pembalikan: " + (d.getKeterangan() != null ? d.getKeterangan() : "")))
                .toList();

        return catat(LocalDate.now(),
                "Pembalikan " + asal.getNomor() + " — " + alasan,
                SumberJurnal.MANUAL,
                asal.getId(),
                baris);
    }

    private BigDecimal nolBilaKosong(BigDecimal nilai) {
        return nilai != null ? nilai : BigDecimal.ZERO;
    }

    /**
     * Nomor jurnal: JRN-yyyyMMdd-XXXX.
     * Memakai suffix acak, bukan nomor urut murni, agar dua pencatatan pada saat
     * bersamaan tidak menabrak batasan unik.
     */
    private String buatNomor(LocalDate tanggal) {
        String tanggalStr = tanggal.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "JRN-" + tanggalStr + "-" + suffix;
    }
}
