package com.projekfajar.akuntansi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.dto.BukuBesarResponse;
import com.projekfajar.akuntansi.dto.JurnalResponse;
import com.projekfajar.akuntansi.dto.LabaRugiResponse;
import com.projekfajar.akuntansi.dto.LabaRugiResponse.BarisAkun;
import com.projekfajar.akuntansi.dto.NeracaResponse;
import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.JurnalDetail;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.model.TipeAkun;
import com.projekfajar.akuntansi.repository.AkunRepository;
import com.projekfajar.akuntansi.repository.JurnalDetailRepository;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Semua laporan keuangan dihitung di sini, selalu dari tabel jurnal.
 *
 * Tidak ada saldo yang disimpan terpisah: begitu ada dua tempat menyimpan angka
 * yang sama, keduanya akan berbeda cepat atau lambat — pelajaran yang sudah
 * didapat dari tabel penghitung produk terjual yang akhirnya dihapus.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LaporanAkuntansiService {

    /** Tanggal pembuka yang cukup jauh ke belakang untuk mencakup seluruh pembukuan. */
    private static final LocalDate AWAL_PEMBUKUAN = LocalDate.of(2000, 1, 1);

    private final JurnalRepository jurnalRepository;
    private final JurnalDetailRepository jurnalDetailRepository;
    private final AkunRepository akunRepository;

    /* ============================================================
       LABA RUGI
    ============================================================ */

    @Transactional(readOnly = true)
    public LabaRugiResponse labaRugi(LocalDate mulai, LocalDate sampai) {
        log.debug("Building profit and loss report for period {} to {}", mulai, sampai);

        // Pendapatan bersaldo normal kredit, jadi arahnya dibalik agar tampil positif
        List<BarisAkun> pendapatan = ringkas(TipeAkun.PENDAPATAN, mulai, sampai, true);
        List<BarisAkun> hpp = ringkas(TipeAkun.HPP, mulai, sampai, false);
        List<BarisAkun> beban = ringkas(TipeAkun.BEBAN, mulai, sampai, false);

        BigDecimal totalPendapatan = jumlahkan(pendapatan);
        BigDecimal totalHpp = jumlahkan(hpp);
        BigDecimal totalBeban = jumlahkan(beban);

        BigDecimal labaKotor = totalPendapatan.subtract(totalHpp);
        int penjualanTanpaHpp = jurnalRepository.penjualanTanpaJurnalHpp(mulai, sampai).size();

        log.info("Profit and loss {} to {}: revenue={}, cogs={}, expense={}, netProfit={}, salesWithoutCogs={}",
                mulai, sampai, totalPendapatan, totalHpp, totalBeban,
                labaKotor.subtract(totalBeban), penjualanTanpaHpp);

        return LabaRugiResponse.builder()
                .mulai(mulai)
                .sampai(sampai)
                .pendapatan(pendapatan)
                .totalPendapatan(totalPendapatan)
                .hpp(hpp)
                .totalHpp(totalHpp)
                .labaKotor(labaKotor)
                .beban(beban)
                .totalBeban(totalBeban)
                .labaBersih(labaKotor.subtract(totalBeban))
                .penjualanTanpaHpp(penjualanTanpaHpp)
                .build();
    }

    /* ============================================================
       NERACA
    ============================================================ */

    @Transactional(readOnly = true)
    public NeracaResponse neraca(LocalDate sampai) {
        log.debug("Building balance sheet as of {}", sampai);

        List<BarisAkun> aset = ringkasSampai(TipeAkun.ASET, sampai, false);
        List<BarisAkun> liabilitas = ringkasSampai(TipeAkun.LIABILITAS, sampai, true);
        List<BarisAkun> ekuitas = ringkasSampai(TipeAkun.EKUITAS, sampai, true);

        BigDecimal totalAset = jumlahkan(aset);
        BigDecimal totalLiabilitas = jumlahkan(liabilitas);
        BigDecimal totalEkuitasTercatat = jumlahkan(ekuitas);

        // Tanpa jurnal penutup, laba periode berjalan belum berpindah ke modal.
        // Karena itu dihitung di sini dari seluruh mutasi sampai tanggal laporan
        // dan ditambahkan ke ekuitas — sehingga neraca tetap seimbang.
        LabaRugiResponse sejakAwal = labaRugi(AWAL_PEMBUKUAN, sampai);
        BigDecimal labaBerjalan = sejakAwal.getLabaBersih();

        BigDecimal totalEkuitas = totalEkuitasTercatat.add(labaBerjalan);
        BigDecimal totalKanan = totalLiabilitas.add(totalEkuitas);
        BigDecimal selisih = totalAset.subtract(totalKanan);
        boolean seimbang = selisih.compareTo(BigDecimal.ZERO) == 0;

        if (seimbang) {
            log.info("Balance sheet as of {}: assets={}, liabilities={}, equity={}, balanced",
                    sampai, totalAset, totalLiabilitas, totalEkuitas);
        } else {
            log.warn("Balance sheet as of {} is NOT balanced: assets={}, liabilities+equity={}, difference={}",
                    sampai, totalAset, totalKanan, selisih);
        }

        return NeracaResponse.builder()
                .sampai(sampai)
                .aset(aset)
                .totalAset(totalAset)
                .liabilitas(liabilitas)
                .totalLiabilitas(totalLiabilitas)
                .ekuitas(ekuitas)
                .labaTahunBerjalan(labaBerjalan)
                .totalEkuitas(totalEkuitas)
                .totalLiabilitasDanEkuitas(totalKanan)
                .selisih(selisih)
                .seimbang(seimbang)
                .build();
    }

    /* ============================================================
       BUKU BESAR
    ============================================================ */

    @Transactional(readOnly = true)
    public BukuBesarResponse bukuBesar(String kodeAkun, LocalDate mulai, LocalDate sampai) {
        log.debug("Building general ledger for account {} period {} to {}", kodeAkun, mulai, sampai);

        Akun akun = akunRepository.findByKode(kodeAkun)
                .orElseThrow(() -> {
                    log.warn("General ledger request rejected: account {} not found", kodeAkun);
                    return new ResourceNotFoundException(
                            "Akun " + kodeAkun + " tidak ditemukan");
                });

        boolean saldoNormalKredit = "KREDIT".equalsIgnoreCase(akun.getSaldoNormal());

        // Repository mengembalikan debit − kredit; akun bersaldo kredit dibalik
        // supaya saldonya terbaca positif seperti di laporan pada umumnya.
        BigDecimal saldoAwal = arah(
                nol(jurnalDetailRepository.saldoAkunSebelum(akun.getId(), mulai)),
                saldoNormalKredit);

        List<JurnalDetail> details = jurnalDetailRepository
                .findMutasiAkun(akun.getId(), mulai, sampai);

        List<BukuBesarResponse.Mutasi> mutasi = new ArrayList<>();
        BigDecimal saldo = saldoAwal;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalKredit = BigDecimal.ZERO;

        for (JurnalDetail detail : details) {
            BigDecimal debit = nol(detail.getDebit());
            BigDecimal kredit = nol(detail.getKredit());

            totalDebit = totalDebit.add(debit);
            totalKredit = totalKredit.add(kredit);
            saldo = saldo.add(arah(debit.subtract(kredit), saldoNormalKredit));

            Jurnal jurnal = detail.getJurnal();

            mutasi.add(BukuBesarResponse.Mutasi.builder()
                    .tanggal(jurnal.getTanggal())
                    .nomorJurnal(jurnal.getNomor())
                    .keterangan(detail.getKeterangan() != null
                            ? detail.getKeterangan()
                            : jurnal.getKeterangan())
                    .debit(debit)
                    .kredit(kredit)
                    .saldo(saldo)
                    .build());
        }

        log.info("General ledger {} ({}) {} to {}: {} mutations, openingBalance={}, closingBalance={}",
                akun.getKode(), akun.getNama(), mulai, sampai, mutasi.size(), saldoAwal, saldo);

        return BukuBesarResponse.builder()
                .kodeAkun(akun.getKode())
                .namaAkun(akun.getNama())
                .saldoNormal(akun.getSaldoNormal())
                .mulai(mulai)
                .sampai(sampai)
                .saldoAwal(saldoAwal)
                .totalDebit(totalDebit)
                .totalKredit(totalKredit)
                .saldoAkhir(saldo)
                .mutasi(mutasi)
                .build();
    }

    /* ============================================================
       JURNAL UMUM
    ============================================================ */

    @Transactional(readOnly = true)
    public List<JurnalResponse> jurnalUmum(LocalDate mulai, LocalDate sampai, SumberJurnal sumber) {
        log.debug("Building general journal for period {} to {}, source={}", mulai, sampai, sumber);

        List<Jurnal> jurnalList = sumber != null
                ? jurnalRepository.findBySumberAndTanggalBetweenOrderByTanggalAscIdAsc(
                        sumber, mulai, sampai)
                : jurnalRepository.findByTanggalBetweenOrderByTanggalAscIdAsc(mulai, sampai);

        List<JurnalResponse> hasil = jurnalList.stream().map(this::keResponse).toList();

        log.info("General journal {} to {} (source={}) returned {} entries",
                mulai, sampai, sumber, hasil.size());

        return hasil;
    }

    /**
     * Penjaga terakhir: seluruh jurnal_detail harus berjumlah nol. Bila tidak,
     * ada data yang masuk lewat jalur di luar JurnalService dan laporan apa pun
     * tidak lagi bisa dipercaya.
     */
    @Transactional(readOnly = true)
    public BigDecimal selisihDebitKredit() {
        BigDecimal selisih = nol(jurnalDetailRepository.selisihDebitKredit());

        if (selisih.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Journal integrity check passed: debit minus credit is zero");
        } else {
            log.warn("Journal integrity check failed: debit minus credit difference={}", selisih);
        }

        return selisih;
    }

    /* ============================================================
       PEMBANTU
    ============================================================ */

    private JurnalResponse keResponse(Jurnal jurnal) {
        List<JurnalResponse.Baris> baris = jurnal.getDetails().stream()
                .map(d -> JurnalResponse.Baris.builder()
                        .kodeAkun(d.getAkun().getKode())
                        .namaAkun(d.getAkun().getNama())
                        .debit(nol(d.getDebit()))
                        .kredit(nol(d.getKredit()))
                        .keterangan(d.getKeterangan())
                        .build())
                .toList();

        return JurnalResponse.builder()
                .id(jurnal.getId())
                .nomor(jurnal.getNomor())
                .tanggal(jurnal.getTanggal())
                .keterangan(jurnal.getKeterangan())
                .sumber(jurnal.getSumber().name())
                .referensiId(jurnal.getReferensiId())
                .totalDebit(jurnal.totalDebit())
                .totalKredit(jurnal.totalKredit())
                .baris(baris)
                .build();
    }

    private List<BarisAkun> ringkas(TipeAkun tipe, LocalDate mulai, LocalDate sampai,
            boolean saldoNormalKredit) {
        return keBarisAkun(jurnalDetailRepository.ringkasPerAkun(tipe, mulai, sampai),
                saldoNormalKredit);
    }

    private List<BarisAkun> ringkasSampai(TipeAkun tipe, LocalDate sampai,
            boolean saldoNormalKredit) {
        return keBarisAkun(jurnalDetailRepository.ringkasPerAkunSampai(tipe, sampai),
                saldoNormalKredit);
    }

    /** Baris repository: [akunId, kode, nama, debit − kredit]. */
    private List<BarisAkun> keBarisAkun(List<Object[]> rows, boolean saldoNormalKredit) {
        List<BarisAkun> hasil = new ArrayList<>();

        for (Object[] row : rows) {
            BigDecimal jumlah = arah((BigDecimal) row[3], saldoNormalKredit);

            // Akun yang mutasinya saling menghapus (misalnya karena jurnal balik)
            // tidak perlu memenuhi laporan
            if (jumlah.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            hasil.add(BarisAkun.builder()
                    .kode((String) row[1])
                    .nama((String) row[2])
                    .jumlah(jumlah)
                    .build());
        }

        return hasil;
    }

    private BigDecimal arah(BigDecimal debitMinusKredit, boolean saldoNormalKredit) {
        BigDecimal nilai = nol(debitMinusKredit);
        return saldoNormalKredit ? nilai.negate() : nilai;
    }

    private BigDecimal jumlahkan(List<BarisAkun> baris) {
        return baris.stream()
                .map(BarisAkun::getJumlah)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nol(BigDecimal nilai) {
        return nilai != null ? nilai : BigDecimal.ZERO;
    }
}
