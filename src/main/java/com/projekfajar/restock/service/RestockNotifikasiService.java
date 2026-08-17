package com.projekfajar.restock.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukVarian;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.produk.repository.ProdukVarianRepository;
import com.projekfajar.restock.dto.RestockNotifikasiRequest;
import com.projekfajar.restock.dto.RestockNotifikasiResponse;
import com.projekfajar.restock.model.RestockNotifikasi;
import com.projekfajar.restock.repository.RestockNotifikasiRepository;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daftar tunggu "beri tahu saya" untuk produk yang sedang kosong.
 *
 * User mendaftar saat stok nol; begitu stok produk atau varian bertambah
 * (restock), seluruh pendaftar yang aktif diberi notifikasi dan daftarnya
 * dinonaktifkan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestockNotifikasiService {

    private final RestockNotifikasiRepository repository;
    private final ProdukRepository produkRepository;
    private final ProdukVarianRepository varianRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public RestockNotifikasiResponse daftar(Long userId, RestockNotifikasiRequest request) {
        log.info("Registering restock subscription: userId={} productId={} variantId={}",
                userId, request.getProdukId(), request.getVariantId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
        Produk produk = produkRepository.findById(request.getProdukId())
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan"));

        ProdukVarian varian = null;
        if (request.getVariantId() != null) {
            varian = varianRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Varian tidak ditemukan"));
        }

        RestockNotifikasi existing = varian != null
                ? repository.findByUserIdAndProdukIdAndVarianId(userId, produk.getId(), varian.getId())
                        .orElse(null)
                : repository.findByUserIdAndProdukIdAndVarianIsNull(userId, produk.getId())
                        .orElse(null);

        if (existing != null) {
            if (Boolean.FALSE.equals(existing.getAktif())) {
                existing.setAktif(true);
                existing.setDikirimAt(null);
                existing.setCreatedAt(LocalDateTime.now());
                existing = repository.save(existing);
                log.info("Reactivated restock subscription id={} userId={} productId={}",
                        existing.getId(), userId, produk.getId());
            } else {
                log.debug("Restock subscription already active: id={} userId={} productId={}",
                        existing.getId(), userId, produk.getId());
            }
            return RestockNotifikasiResponse.from(existing);
        }

        RestockNotifikasi baru = RestockNotifikasi.builder()
                .user(user)
                .produk(produk)
                .varian(varian)
                .aktif(true)
                .createdAt(LocalDateTime.now())
                .build();

        RestockNotifikasi saved = repository.save(baru);
        log.info("Restock subscription created: id={} userId={} productId={} variantId={}",
                saved.getId(), userId, produk.getId(), varian != null ? varian.getId() : null);
        return RestockNotifikasiResponse.from(saved);
    }

    @Transactional
    public void batal(Long userId, Long id) {
        log.info("Cancelling restock subscription id={} for userId={}", id, userId);
        RestockNotifikasi notifikasi = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notifikasi tidak ditemukan"));

        if (!notifikasi.getUser().getId().equals(userId)) {
            log.warn("Access denied: userId={} tried to cancel restock subscription id={} owned by userId={}",
                    userId, id, notifikasi.getUser().getId());
            throw new com.projekfajar.exception.BusinessException(
                    "Anda tidak memiliki akses ke notifikasi ini");
        }

        notifikasi.setAktif(false);
        repository.save(notifikasi);
        log.info("Restock subscription cancelled: id={} userId={}", id, userId);
    }

    @Transactional(readOnly = true)
    public List<RestockNotifikasiResponse> getByUser(Long userId) {
        log.debug("Fetching restock subscriptions for userId={}", userId);
        List<RestockNotifikasiResponse> hasil = repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RestockNotifikasiResponse::from)
                .toList();
        log.debug("Found {} restock subscriptions for userId={}", hasil.size(), userId);
        return hasil;
    }

    /**
     * Dipanggil setiap stok produk berubah. Bila stok kembali tersedia, kirim
     * notifikasi ke semua pendaftar aktif produk itu (dasar maupun per varian).
     * Aman dipanggil berulang: pendaftar yang sudah diberi tahu tidak diulang.
     */
    @Transactional
    public void cekRestock(Produk produk) {
        if (produk == null || produk.getId() == null) {
            log.debug("Skipping restock check: product is null or not persisted");
            return;
        }

        log.debug("Checking restock for productId={} nama={}", produk.getId(), produk.getNama());
        boolean stokTersedia = stokProduk(produk) > 0;
        if (!stokTersedia) {
            log.debug("Product {} still out of stock, no restock notification sent", produk.getId());
            return;
        }

        List<RestockNotifikasi> pendaftar = new ArrayList<>(
                repository.findByProdukIdAndVarianIsNullAndAktifTrue(produk.getId()));

        Set<Long> varianIdsSudah = new HashSet<>();
        List<ProdukVarian> varianList = varianRepository.findByProdukIdOrderByIdAsc(produk.getId());
        for (ProdukVarian varian : varianList) {
            if (varian.getStock() != null && varian.getStock() > 0) {
                pendaftar.addAll(repository.findByVarianIdAndAktifTrue(varian.getId()));
                varianIdsSudah.add(varian.getId());
            }
        }

        if (pendaftar.isEmpty()) {
            log.debug("No active restock subscribers for productId={}", produk.getId());
            return;
        }

        int terkirim = 0;
        for (RestockNotifikasi notifikasi : pendaftar) {
            String namaVariant = notifikasi.getVarian() != null
                    ? notifikasi.getVarian().getNama()
                    : null;
            notificationService.sendRestockNotification(
                    notifikasi.getUser(), produk.getNama(), namaVariant, produk.getId());

            notifikasi.setAktif(false);
            notifikasi.setDikirimAt(LocalDateTime.now());
            repository.save(notifikasi);
            terkirim++;
        }

        log.info("Restock notifications sent: productId={} nama={} sent={}/{}",
                produk.getId(), produk.getNama(), terkirim, pendaftar.size());
    }

    /** Bila ada varian tersedia, produk dianggap tersedia. */
    private int stokProduk(Produk produk) {
        int stokDasar = produk.getStock() != null ? produk.getStock() : 0;
        int stokVarian = varianRepository.findByProdukIdOrderByIdAsc(produk.getId()).stream()
                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                .sum();
        return stokDasar + stokVarian;
    }
}
