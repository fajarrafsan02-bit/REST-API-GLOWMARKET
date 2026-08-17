package com.projekfajar.akuntansi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.model.PesananItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Menjurnal penjualan yang sudah lunas.
 *
 * Dipanggil di dalam transaksi yang sama dengan perubahan status pesanan, agar
 * pesanan dan pembukuannya tidak pernah terpisah: bila jurnal gagal, status
 * pesanan ikut batal dan webhook Xendit akan mencoba lagi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostingPenjualanService {

    public static final String AKUN_KAS = "1-100";
    public static final String AKUN_PERSEDIAAN = "1-200";
    public static final String AKUN_PENDAPATAN = "4-100";
    public static final String AKUN_PENDAPATAN_ONGKIR = "4-200";
    public static final String AKUN_POTONGAN_PENJUALAN = "4-300";
    public static final String AKUN_HPP = "5-100";

    private final JurnalService jurnalService;
    private final JurnalRepository jurnalRepository;

    @Transactional
    public void catatPenjualan(Pesanan pesanan) {
        // Webhook Xendit bisa datang lebih dari sekali; jangan menjurnal ganda.
        if (jurnalRepository.existsBySumberAndReferensiId(SumberJurnal.PENJUALAN, pesanan.getId())) {
            log.info("Penjualan {} sudah dijurnal sebelumnya", pesanan.getNomorPesanan());
            return;
        }

        BigDecimal total = nol(pesanan.getTotalHarga());
        BigDecimal ongkir = nol(pesanan.getOngkir());
        BigDecimal diskon = nol(pesanan.getDiskon());
        BigDecimal pendapatanBarang = total.subtract(ongkir).add(diskon);

        if (total.signum() <= 0) {
            log.warn("Pesanan {} bernilai nol, tidak dijurnal", pesanan.getNomorPesanan());
            return;
        }

        LocalDate tanggal = pesanan.getCreatedAt().toLocalDate();
        String keterangan = "Penjualan pesanan " + pesanan.getNomorPesanan();

        List<Baris> baris = new ArrayList<>();
        baris.add(Baris.debit(AKUN_KAS, total, keterangan));
        baris.add(Baris.kredit(AKUN_PENDAPATAN, pendapatanBarang, keterangan));

        // Diskon adalah kontra-pendapatan: Kas lebih kecil, Pendapatan tetap
        // utuh, dan potongannya dicatat terpisah agar laporan laba-rugi bisa
        // melihat berapa besar diskon yang sebenarnya diberikan.
        if (diskon.signum() > 0) {
            baris.add(Baris.debit(AKUN_POTONGAN_PENJUALAN, diskon,
                    "Potongan voucher " + pesanan.getNomorPesanan()));
        }

        if (ongkir.signum() > 0) {
            baris.add(Baris.kredit(AKUN_PENDAPATAN_ONGKIR, ongkir, "Ongkir " + pesanan.getNomorPesanan()));
        }

        jurnalService.catat(tanggal, keterangan, SumberJurnal.PENJUALAN, pesanan.getId(), baris);

        catatHpp(pesanan, tanggal);
    }

    /**
     * Barang retur sudah kembali ke gudang: persediaan buku naik lagi, HPP
     * turun. Dipakai saat pengembalian berstatus DITERIMA — bukan saat uang
     * di-refund — supaya neraca tidak menampilkan stok yang belum fisik ada.
     *
     * Jurnal penjualan asal tetap berdiri; ini entri baru sumber REFUND.
     */
    @Transactional
    public void catatPengembalianHpp(Pesanan pesanan, Long pengembalianId, String nomorPengembalian,
            LocalDate tanggal) {
        if (sudahAdaJurnalHppPengembalian(pengembalianId)) {
            log.info("HPP pengembalian {} sudah dijurnal sebelumnya", nomorPengembalian);
            return;
        }

        BigDecimal totalModal = hitungModal(pesanan);
        if (totalModal.signum() <= 0) {
            log.warn("HPP pengembalian {} tidak dijurnal: harga modal produknya belum diisi",
                    nomorPengembalian);
            return;
        }

        String keterangan = "HPP pengembalian " + nomorPengembalian
                + " (pesanan " + pesanan.getNomorPesanan() + ")";
        jurnalService.catat(tanggal, keterangan, SumberJurnal.REFUND, pengembalianId, List.of(
                Baris.debit(AKUN_PERSEDIAAN, totalModal, keterangan),
                Baris.kredit(AKUN_HPP, totalModal, keterangan)));
    }

    /**
     * Membatalkan pembukuan sebuah penjualan, dipakai saat pesanan yang sudah
     * dibayar dibatalkan admin.
     *
     * Seluruh jurnal penjualan itu dibalik — pendapatan, ongkir, dan HPP-nya —
     * sehingga kas berkurang kembali dan barang masuk lagi ke persediaan.
     * Jurnal asalnya tetap berdiri: koreksi pembukuan selalu berupa entri baru.
     */
    @Transactional
    public void batalkanPenjualan(Pesanan pesanan, String alasan) {
        List<Jurnal> jurnalAsal = jurnalRepository
                .findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.PENJUALAN, pesanan.getId());

        if (jurnalAsal.isEmpty()) {
            log.info("Pesanan {} tidak punya jurnal penjualan, tidak ada yang dibalik",
                    pesanan.getNomorPesanan());
            return;
        }

        for (Jurnal jurnal : jurnalAsal) {
            jurnalService.catatBalik(jurnal,
                    alasan != null ? alasan : "pesanan dibatalkan");
        }

        log.info("Pembukuan penjualan {} dibalik: {}", pesanan.getNomorPesanan(), alasan);
    }

    /**
     * HPP hanya dijurnal bila harga modalnya diketahui. Produk lama yang modalnya
     * masih nol sengaja dilewati — lebih baik tidak mencatat daripada mencatat
     * laba yang salah, dan kondisinya dilaporkan lewat log.
     */
    private void catatHpp(Pesanan pesanan, LocalDate tanggal) {
        BigDecimal totalModal = hitungModal(pesanan);

        if (totalModal.signum() <= 0) {
            log.warn("HPP pesanan {} tidak dijurnal: harga modal produknya belum diisi",
                    pesanan.getNomorPesanan());
            return;
        }

        String keterangan = "HPP pesanan " + pesanan.getNomorPesanan();

        jurnalService.catat(tanggal, keterangan, SumberJurnal.PENJUALAN, pesanan.getId(), List.of(
                Baris.debit(AKUN_HPP, totalModal, keterangan),
                Baris.kredit(AKUN_PERSEDIAAN, totalModal, keterangan)));
    }

    private boolean sudahAdaJurnalHppPengembalian(Long pengembalianId) {
        return jurnalRepository
                .findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.REFUND, pengembalianId)
                .stream()
                .flatMap(jurnal -> jurnal.getDetails().stream())
                .anyMatch(detail -> detail.getAkun() != null
                        && AKUN_HPP.equals(detail.getAkun().getKode())
                        && detail.getKredit() != null
                        && detail.getKredit().signum() > 0);
    }

    private BigDecimal hitungModal(Pesanan pesanan) {
        BigDecimal totalModal = BigDecimal.ZERO;
        for (PesananItem item : pesanan.getItems()) {
            BigDecimal modal = nol(item.getHargaModal());
            totalModal = totalModal.add(modal.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return totalModal;
    }

    private BigDecimal nol(BigDecimal nilai) {
        return nilai != null ? nilai : BigDecimal.ZERO;
    }
}
