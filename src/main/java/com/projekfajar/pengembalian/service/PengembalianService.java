package com.projekfajar.pengembalian.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.akuntansi.service.PostingPenjualanService;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.pengembalian.dto.PengembalianRequest;
import com.projekfajar.pengembalian.dto.PengembalianResponse;
import com.projekfajar.pengembalian.model.Pengembalian;
import com.projekfajar.pengembalian.model.PengembalianStatus;
import com.projekfajar.pengembalian.repository.PengembalianRepository;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.model.PesananItem;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.produk.repository.ProdukVarianRepository;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.terjual.model.ProdukTerjual;
import com.projekfajar.terjual.repository.ProdukTerjualRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Alur pengembalian barang & refund.
 *
 * Pembeli mengajukan untuk pesanan yang sudah SELESAI. Admin menyetujui
 * (uang dikembalikan, jurnal kas + pendapatan) atau menolak. Saat barang
 * sudah diterima kembali, admin menandai DITERIMA: stok fisik dipulihkan
 * dan HPP/persediaan dibalik di pembukuan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PengembalianService {

    private static final String AKUN_KAS = "1-100";
    private static final String AKUN_PENDAPATAN = "4-100";
    private static final String AKUN_PENDAPATAN_ONGKIR = "4-200";
    private static final String AKUN_POTONGAN_PENJUALAN = "4-300";

    private final PengembalianRepository pengembalianRepository;
    private final PesananRepository pesananRepository;
    private final ProdukRepository produkRepository;
    private final ProdukVarianRepository varianRepository;
    private final JurnalService jurnalService;
    private final JurnalRepository jurnalRepository;
    private final PostingPenjualanService postingPenjualanService;
    private final ProdukTerjualRepository produkTerjualRepository;
    private final NotificationService notificationService;
    private final com.projekfajar.restock.service.RestockNotifikasiService restockNotifikasiService;

    @Transactional
    public PengembalianResponse ajukan(Long userId, PengembalianRequest request) {
        Pesanan pesanan = pesananRepository.findById(request.getPesananId())
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));

        if (!pesanan.getUser().getId().equals(userId)) {
            throw new BusinessException("Anda tidak memiliki akses ke pesanan ini");
        }

        if (pesanan.getStatus() != OrderStatus.SELESAI) {
            throw new BusinessException(
                    "Pengembalian hanya bisa diajukan untuk pesanan yang sudah selesai");
        }

        boolean sudahAda = pengembalianRepository.existsByPesananIdAndStatusIn(
                pesanan.getId(), List.of(PengembalianStatus.DIAJUKAN, PengembalianStatus.DISETUJUI));
        if (sudahAda) {
            throw new BusinessException("Pengembalian untuk pesanan ini sudah pernah diajukan");
        }

        Pengembalian pengembalian = Pengembalian.builder()
                .nomorPengembalian(generateNomorPengembalian())
                .pesanan(pesanan)
                .user(pesanan.getUser())
                .alasan(request.getAlasan().trim())
                .status(PengembalianStatus.DIAJUKAN)
                .jumlahRefund(pesanan.getTotalHarga())
                .createdAt(LocalDateTime.now())
                .build();

        Pengembalian saved = pengembalianRepository.save(pengembalian);
        log.info("Pengajuan pengembalian {} untuk pesanan {}", saved.getNomorPengembalian(),
                pesanan.getNomorPesanan());

        return PengembalianResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PengembalianResponse> getByUser(Long userId) {
        return pengembalianRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PengembalianResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PengembalianResponse> getAll(String status) {
        List<Pengembalian> list = (status == null || status.isBlank())
                ? pengembalianRepository.findAll()
                : pengembalianRepository.findByStatusOrderByCreatedAtDesc(
                        PengembalianStatus.valueOf(status.trim().toUpperCase()));
        return list.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(PengembalianResponse::from)
                .toList();
    }

    @Transactional
    public PengembalianResponse setujui(Long id, String catatan) {
        Pengembalian pengembalian = ambil(id);
        pastikanStatus(pengembalian, PengembalianStatus.DIAJUKAN);

        LocalDateTime now = LocalDateTime.now();
        pengembalian.setStatus(PengembalianStatus.DISETUJUI);
        pengembalian.setJumlahRefund(pengembalian.getPesanan().getTotalHarga());
        pengembalian.setCatatanAdmin(kosongkan(catatan));
        pengembalian.setApprovedAt(now);
        pengembalian.setUpdatedAt(now);

        Pengembalian saved = pengembalianRepository.save(pengembalian);
        
        Pesanan pesanan = saved.getPesanan();
        pesanan.setStatus(OrderStatus.DIKEMBALIKAN);
        pesananRepository.save(pesanan);
        hapusCatatanPenjualan(pesanan);

        jurnalRefund(saved);
        log.info("Pengembalian {} disetujui, uang {} dikembalikan",
                saved.getNomorPengembalian(), saved.getJumlahRefund());

        notificationService.sendPengembalianNotification(saved, true);
        return PengembalianResponse.from(saved);
    }

    @Transactional
    public PengembalianResponse tolak(Long id, String catatan) {
        Pengembalian pengembalian = ambil(id);
        pastikanStatus(pengembalian, PengembalianStatus.DIAJUKAN);

        LocalDateTime now = LocalDateTime.now();
        pengembalian.setStatus(PengembalianStatus.DITOLAK);
        pengembalian.setCatatanAdmin(kosongkan(catatan));
        pengembalian.setUpdatedAt(now);

        Pengembalian saved = pengembalianRepository.save(pengembalian);
        log.info("Pengembalian {} ditolak", saved.getNomorPengembalian());

        notificationService.sendPengembalianNotification(saved, false);
        return PengembalianResponse.from(saved);
    }

    @Transactional
    public PengembalianResponse terima(Long id) {
        Pengembalian pengembalian = ambil(id);
        pastikanStatus(pengembalian, PengembalianStatus.DISETUJUI);

        LocalDateTime now = LocalDateTime.now();
        pengembalian.setStatus(PengembalianStatus.DITERIMA);
        pengembalian.setDiterimaAt(now);
        pengembalian.setUpdatedAt(now);

        Pengembalian saved = pengembalianRepository.save(pengembalian);
        
        Pesanan pesanan = saved.getPesanan();
        if (pesanan.getStatus() != OrderStatus.DIKEMBALIKAN) {
            pesanan.setStatus(OrderStatus.DIKEMBALIKAN);
            pesananRepository.save(pesanan);
        }

        pulihkanStok(saved.getPesanan());
        postingPenjualanService.catatPengembalianHpp(
                saved.getPesanan(), saved.getId(), saved.getNomorPengembalian(), now.toLocalDate());
        hapusCatatanPenjualan(saved.getPesanan());
        log.info("Barang pengembalian {} diterima, stok dan HPP dipulihkan",
                saved.getNomorPengembalian());

        return PengembalianResponse.from(saved);
    }

    private Pengembalian ambil(Long id) {
        return pengembalianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pengembalian tidak ditemukan"));
    }

    private void pastikanStatus(Pengembalian pengembalian, PengembalianStatus diharapkan) {
        if (pengembalian.getStatus() != diharapkan) {
            throw new BusinessException("Status pengembalian tidak bisa diproses (" +
                    pengembalian.getStatus() + ")");
        }
    }

    /**
     * Mencatat refund sebagai entri baru (jurnal asal penjualan tetap berdiri).
     * Setara dengan kebalikan penjualan: kas keluar, pendapatan & ongkir
     * dikurangi, potongan voucher dikembalikan agar bersih.
     */
    private void jurnalRefund(Pengembalian pengembalian) {
        Pesanan pesanan = pengembalian.getPesanan();

        if (jurnalRepository.existsBySumberAndReferensiId(SumberJurnal.REFUND, pengembalian.getId())) {
            log.info("Refund {} sudah dijurnal sebelumnya", pengembalian.getNomorPengembalian());
            return;
        }

        BigDecimal total = nol(pesanan.getTotalHarga());
        BigDecimal ongkir = nol(pesanan.getOngkir());
        BigDecimal diskon = nol(pesanan.getDiskon());
        BigDecimal pendapatanBarang = total.subtract(ongkir).add(diskon);

        LocalDate tanggal = pengembalian.getApprovedAt() != null
                ? pengembalian.getApprovedAt().toLocalDate()
                : LocalDate.now();
        String keterangan = "Refund pengembalian " + pengembalian.getNomorPengembalian()
                + " (pesanan " + pesanan.getNomorPesanan() + ")";

        List<Baris> baris = new ArrayList<>();
        baris.add(Baris.kredit(AKUN_KAS, total, keterangan));
        baris.add(Baris.debit(AKUN_PENDAPATAN, pendapatanBarang, keterangan));

        if (diskon.signum() > 0) {
            baris.add(Baris.kredit(AKUN_POTONGAN_PENJUALAN, diskon, keterangan));
        }

        if (ongkir.signum() > 0) {
            baris.add(Baris.debit(AKUN_PENDAPATAN_ONGKIR, ongkir, keterangan));
        }

        jurnalService.catat(tanggal, keterangan, SumberJurnal.REFUND, pengembalian.getId(), baris);
    }

    /**
     * Retur yang disetujui bukan penjualan jadi. Catatan terjual dicabut
     * supaya katalog, dashboard, dan laporan tidak menghitung barang itu.
     */
    private void hapusCatatanPenjualan(Pesanan pesanan) {
        List<ProdukTerjual> catatan = produkTerjualRepository.findByPesananId(pesanan.getId());
        if (!catatan.isEmpty()) {
            produkTerjualRepository.deleteAll(catatan);
            log.info("Catatan terjual dicabut untuk pesanan {} ({} baris)",
                    pesanan.getNomorPesanan(), catatan.size());
        }
    }

    /** Barang sudah kembali ke toko: stok produk/varian dipulihkan. */
    private void pulihkanStok(Pesanan pesanan) {
        for (PesananItem item : pesanan.getItems()) {
            if (item.getVariant() != null) {
                var varian = item.getVariant();
                varian.setStock(varian.getStock() + item.getQuantity());
                varianRepository.save(varian);
                restockNotifikasiService.cekRestock(varian.getProduk());
            } else {
                var produk = item.getProduk();
                produk.setStock(produk.getStock() + item.getQuantity());
                produkRepository.save(produk);
                restockNotifikasiService.cekRestock(produk);
            }
        }
    }

    private String generateNomorPengembalian() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "RTRN-" + timestamp + "-" + suffix;
    }

    private String kosongkan(String nilai) {
        if (nilai == null) {
            return null;
        }
        String trimmed = nilai.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal nol(BigDecimal nilai) {
        return nilai != null ? nilai : BigDecimal.ZERO;
    }
}
