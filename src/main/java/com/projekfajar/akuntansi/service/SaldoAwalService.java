package com.projekfajar.akuntansi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.dto.SaldoAwalInfo;
import com.projekfajar.akuntansi.dto.SaldoAwalRequest;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Titik mulai pembukuan.
 *
 * Kas dan stok toko sudah ada sebelum modul ini dipasang, jadi tanpa jurnal
 * pembuka neraca mustahil seimbang: persediaan akan berkurang karena HPP
 * padahal tidak pernah tercatat masuk.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaldoAwalService {

    public static final String AKUN_KAS = "1-100";
    public static final String AKUN_PERSEDIAAN = "1-200";
    public static final String AKUN_MODAL = "3-100";

    private final JurnalService jurnalService;
    private final JurnalRepository jurnalRepository;
    private final ProdukRepository produkRepository;

    @Transactional(readOnly = true)
    public SaldoAwalInfo info() {
        log.debug("Reading opening balance info");

        List<Produk> produkAktif = produkRepository.findByDeletedFalse();

        long tanpaModal = produkAktif.stream()
                .filter(p -> nol(p.getHargaModal()).signum() <= 0)
                .count();

        boolean sudahDicatat = jurnalRepository.existsBySumber(SumberJurnal.SALDO_AWAL);
        BigDecimal nilaiPersediaan = hitungNilaiPersediaan(produkAktif);

        log.info("Opening balance info: recorded={}, inventoryValue={}, products={}, withoutCostPrice={}",
                sudahDicatat, nilaiPersediaan, produkAktif.size(), tanpaModal);

        return SaldoAwalInfo.builder()
                .sudahDicatat(sudahDicatat)
                .nilaiPersediaan(nilaiPersediaan)
                .produkTanpaModal(tanpaModal)
                .totalProduk(produkAktif.size())
                .build();
    }

    /**
     * Hanya boleh sekali. Kalau angkanya keliru, koreksinya lewat jurnal manual —
     * bukan dengan menimpa jurnal pembuka, karena semua laporan setelahnya berdiri
     * di atas angka ini.
     */
    @Transactional
    public Jurnal catat(SaldoAwalRequest request) {
        log.info("Recording opening balance: date={}, cash={}", request.getTanggal(), request.getKas());

        if (jurnalRepository.existsBySumber(SumberJurnal.SALDO_AWAL)) {
            log.warn("Opening balance rejected: already recorded before, use manual journal for correction");
            throw new BusinessException(
                    "Saldo awal sudah pernah dicatat. Gunakan jurnal manual untuk koreksi.");
        }

        BigDecimal kas = nol(request.getKas());
        if (kas.signum() < 0) {
            log.warn("Opening balance rejected: negative cash amount {}", kas);
            throw new BusinessException("Saldo kas tidak boleh negatif");
        }

        BigDecimal persediaan = hitungNilaiPersediaan(produkRepository.findByDeletedFalse());
        BigDecimal total = kas.add(persediaan);

        if (total.signum() <= 0) {
            log.warn("Opening balance rejected: total is zero (cash={}, inventory={})", kas, persediaan);
            throw new BusinessException(
                    "Saldo awal bernilai nol. Isi saldo kas atau harga modal produk terlebih dahulu.");
        }

        LocalDate tanggal = request.getTanggal() != null ? request.getTanggal() : LocalDate.now();
        String keterangan = "Saldo awal pembukuan";

        List<Baris> baris = new ArrayList<>();
        if (kas.signum() > 0) {
            baris.add(Baris.debit(AKUN_KAS, kas, "Saldo kas awal"));
        }
        if (persediaan.signum() > 0) {
            baris.add(Baris.debit(AKUN_PERSEDIAAN, persediaan, "Nilai persediaan awal"));
        }
        baris.add(Baris.kredit(AKUN_MODAL, total, keterangan));

        Jurnal jurnal = jurnalService.catat(tanggal, keterangan, SumberJurnal.SALDO_AWAL, null, baris);

        log.info("Opening balance recorded: journalId={}, number={}, date={}, cash={}, inventory={}, total={}",
                jurnal.getId(), jurnal.getNomor(), tanggal, kas, persediaan, total);

        return jurnal;
    }

    private BigDecimal hitungNilaiPersediaan(List<Produk> produkAktif) {
        BigDecimal nilai = BigDecimal.ZERO;

        for (Produk produk : produkAktif) {
            int stok = produk.getStock() != null ? produk.getStock() : 0;
            if (stok <= 0) {
                continue;
            }
            nilai = nilai.add(nol(produk.getHargaModal()).multiply(BigDecimal.valueOf(stok)));
        }

        return nilai;
    }

    private BigDecimal nol(BigDecimal nilai) {
        return nilai != null ? nilai : BigDecimal.ZERO;
    }
}
