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

import com.projekfajar.akuntansi.dto.PembelianRequest;
import com.projekfajar.akuntansi.dto.PembelianResponse;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.MetodePembelian;
import com.projekfajar.akuntansi.model.Pembelian;
import com.projekfajar.akuntansi.model.PembelianItem;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.repository.PembelianRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pembelian stok: satu tindakan yang sekaligus menambah persediaan barang,
 * memperbarui harga modal, dan mencatat uang keluar atau utang ke pemasok.
 *
 * Ketiganya berada dalam satu transaksi supaya tidak pernah terjadi stok
 * bertambah tanpa pembukuan, atau sebaliknya.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PembelianService {

    public static final String AKUN_KAS = "1-100";
    public static final String AKUN_PERSEDIAAN = "1-200";
    public static final String AKUN_UTANG = "2-100";

    private final PembelianRepository pembelianRepository;
    private final ProdukRepository produkRepository;
    private final JurnalRepository jurnalRepository;
    private final JurnalService jurnalService;

    @Transactional(readOnly = true)
    public List<PembelianResponse> daftar(LocalDate mulai, LocalDate sampai) {
        return pembelianRepository
                .findByTanggalBetweenOrderByTanggalDescIdDesc(mulai, sampai)
                .stream()
                .map(this::keResponse)
                .toList();
    }

    @Transactional
    public PembelianResponse buat(PembelianRequest request) {
        LocalDate tanggal = request.getTanggal() != null ? request.getTanggal() : LocalDate.now();
        MetodePembelian metode = request.getMetode() != null
                ? request.getMetode()
                : MetodePembelian.TUNAI;
        boolean tunai = metode == MetodePembelian.TUNAI;
        LocalDateTime sekarang = LocalDateTime.now();

        Pembelian pembelian = Pembelian.builder()
                .nomor(buatNomor(tanggal))
                .tanggal(tanggal)
                .pemasok(kosongJadiNull(request.getPemasok()))
                .catatan(kosongJadiNull(request.getCatatan()))
                .metode(metode)
                .dilunasi(tunai)
                .dilunasiAt(tunai ? sekarang : null)
                .total(BigDecimal.ZERO)
                .createdAt(sekarang)
                .build();

        List<PembelianItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PembelianRequest.Item baris : request.getItems()) {
            Produk produk = produkRepository.findByIdAndDeletedFalse(baris.getProdukId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produk " + baris.getProdukId() + " tidak ditemukan"));

            BigDecimal subtotal = baris.getHargaBeli()
                    .multiply(BigDecimal.valueOf(baris.getQty()));

            items.add(PembelianItem.builder()
                    .pembelian(pembelian)
                    .produk(produk)
                    .namaProduk(produk.getNama())
                    .qty(baris.getQty())
                    .hargaBeli(baris.getHargaBeli())
                    .subtotal(subtotal)
                    .build());

            total = total.add(subtotal);

            // Stok bertambah dan harga modal mengikuti harga beli terakhir.
            // Pesanan yang sudah terjadi tidak terpengaruh karena modalnya sudah di-snapshot.
            produk.setStock(produk.getStock() + baris.getQty());
            produk.setHargaModal(baris.getHargaBeli());
            produk.setUpdatedAt(LocalDateTime.now());
            produkRepository.save(produk);
        }

        if (total.signum() <= 0) {
            throw new BusinessException("Nilai pembelian harus lebih dari nol");
        }

        pembelian.setItems(items);
        pembelian.setTotal(total);

        Pembelian tersimpan = pembelianRepository.save(pembelian);

        String keterangan = "Pembelian " + tersimpan.getNomor()
                + (tersimpan.getPemasok() != null ? " dari " + tersimpan.getPemasok() : "")
                + (tunai ? "" : " (kredit)");

        String akunKredit = tunai ? AKUN_KAS : AKUN_UTANG;

        jurnalService.catat(tanggal, keterangan, SumberJurnal.PEMBELIAN, tersimpan.getId(), List.of(
                Baris.debit(AKUN_PERSEDIAAN, total, keterangan),
                Baris.kredit(akunKredit, total, keterangan)));

        log.info("Pembelian {} tercatat ({}): {} barang, total {}",
                tersimpan.getNomor(), metode, items.size(), total);

        return keResponse(tersimpan);
    }

    /**
     * Pembatalan mengembalikan stok dan membuat jurnal balik. Jurnal asal tetap
     * berdiri: pembukuan mengoreksi dengan entri baru, bukan dengan menghapus jejak.
     */
    @Transactional
    public PembelianResponse batalkan(Long id, String alasan) {
        Pembelian pembelian = pembelianRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pembelian tidak ditemukan"));

        if (Boolean.TRUE.equals(pembelian.getDibatalkan())) {
            throw new BusinessException("Pembelian ini sudah dibatalkan");
        }

        for (PembelianItem item : pembelian.getItems()) {
            Produk produk = item.getProduk();
            int sisa = produk.getStock() - item.getQty();

            if (sisa < 0) {
                throw new BusinessException(
                        "Pembelian tidak bisa dibatalkan: stok " + produk.getNama()
                                + " sudah terjual, tersisa " + produk.getStock()
                                + " dari " + item.getQty() + " yang dibeli");
            }

            produk.setStock(sisa);
            produk.setUpdatedAt(LocalDateTime.now());
            produkRepository.save(produk);
        }

        List<Jurnal> jurnalAsal = new ArrayList<>();
        jurnalAsal.addAll(jurnalRepository
                .findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.PEMBELIAN, pembelian.getId()));
        jurnalAsal.addAll(jurnalRepository
                .findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal.PELUNASAN, pembelian.getId()));

        for (Jurnal jurnal : jurnalAsal) {
            jurnalService.catatBalik(jurnal, alasan != null ? alasan : "pembelian dibatalkan");
        }

        pembelian.setDibatalkan(true);
        pembelian.setDibatalkanAt(LocalDateTime.now());

        log.info("Pembelian {} dibatalkan: {}", pembelian.getNomor(), alasan);

        // Harga modal produk sengaja tidak dikembalikan ke nilai sebelumnya:
        // nilai lama tidak disimpan di mana pun, dan menebaknya justru berisiko
        // membuat HPP penjualan berikutnya salah. Admin bisa memperbaikinya di form produk.
        return keResponse(pembelianRepository.save(pembelian));
    }

    /**
     * Melunasi pembelian kredit: utang turun, kas keluar. Stok tidak berubah —
     * barangnya sudah masuk saat pembelian dicatat.
     */
    @Transactional
    public PembelianResponse lunasi(Long id) {
        Pembelian pembelian = pembelianRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pembelian tidak ditemukan"));

        if (Boolean.TRUE.equals(pembelian.getDibatalkan())) {
            throw new BusinessException("Pembelian yang dibatalkan tidak bisa dilunasi");
        }

        if (pembelian.getMetode() != MetodePembelian.KREDIT) {
            throw new BusinessException("Pembelian tunai sudah lunas saat dicatat");
        }

        if (Boolean.TRUE.equals(pembelian.getDilunasi())) {
            throw new BusinessException("Utang pembelian ini sudah dilunasi");
        }

        String keterangan = "Pelunasan utang " + pembelian.getNomor()
                + (pembelian.getPemasok() != null ? " ke " + pembelian.getPemasok() : "");

        jurnalService.catat(LocalDate.now(), keterangan, SumberJurnal.PELUNASAN, pembelian.getId(), List.of(
                Baris.debit(AKUN_UTANG, pembelian.getTotal(), keterangan),
                Baris.kredit(AKUN_KAS, pembelian.getTotal(), keterangan)));

        pembelian.setDilunasi(true);
        pembelian.setDilunasiAt(LocalDateTime.now());

        log.info("Pembelian {} dilunasi: kas keluar {}",
                pembelian.getNomor(), pembelian.getTotal());

        return keResponse(pembelianRepository.save(pembelian));
    }

    private PembelianResponse keResponse(Pembelian pembelian) {
        List<PembelianResponse.Item> items = pembelian.getItems().stream()
                .map(item -> PembelianResponse.Item.builder()
                        .produkId(item.getProduk() != null ? item.getProduk().getId() : null)
                        .namaProduk(item.getNamaProduk())
                        .qty(item.getQty())
                        .hargaBeli(item.getHargaBeli())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return PembelianResponse.builder()
                .id(pembelian.getId())
                .nomor(pembelian.getNomor())
                .tanggal(pembelian.getTanggal())
                .pemasok(pembelian.getPemasok())
                .total(pembelian.getTotal())
                .metode(pembelian.getMetode())
                .dilunasi(Boolean.TRUE.equals(pembelian.getDilunasi()))
                .dilunasiAt(pembelian.getDilunasiAt())
                .catatan(pembelian.getCatatan())
                .dibatalkan(Boolean.TRUE.equals(pembelian.getDibatalkan()))
                .createdAt(pembelian.getCreatedAt())
                .items(items)
                .build();
    }

    private String kosongJadiNull(String nilai) {
        return nilai != null && !nilai.isBlank() ? nilai.trim() : null;
    }

    /** BLI-yyyyMMdd-XXXX, pola yang sama dengan nomor jurnal dan nomor pesanan. */
    private String buatNomor(LocalDate tanggal) {
        String tanggalStr = tanggal.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "BLI-" + tanggalStr + "-" + suffix;
    }
}
