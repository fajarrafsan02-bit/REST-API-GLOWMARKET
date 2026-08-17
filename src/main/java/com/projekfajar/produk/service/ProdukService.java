package com.projekfajar.produk.service;

import com.projekfajar.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.projekfajar.akuntansi.service.PenyesuaianStokService;
import com.projekfajar.produk.dto.ProdukRequest;
import com.projekfajar.produk.dto.ProdukResponse;
import com.projekfajar.produk.dto.ProdukVarianResponse;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukGambar;
import com.projekfajar.produk.model.ProdukVarian;
import com.projekfajar.produk.model.StatusProduk;
import com.projekfajar.keranjang.repository.KeranjangRepository;
import com.projekfajar.produk.repository.ProdukGambarRepository;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.produk.repository.ProdukVarianRepository;
import com.projekfajar.pesanan.repository.PesananItemRepository;
import com.projekfajar.terjual.repository.ProdukTerjualRepository;
import com.projekfajar.wishlist.repository.WishlistRepository;
import com.projekfajar.settings.service.SettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdukService {
    private final ProdukRepository produkRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final SettingService settingService;
    private final KeranjangRepository keranjangRepository;
    private final WishlistRepository wishlistRepository;
    private final ProdukTerjualRepository produkTerjualRepository;
    private final ProdukVarianRepository varianRepository;
    private final ProdukGambarRepository gambarRepository;
    private final PesananItemRepository pesananItemRepository;
    private final ProdukVarianService varianService;
    private final com.projekfajar.restock.service.RestockNotifikasiService restockNotifikasiService;

    /**
     * Setiap perubahan stok yang lewat service ini adalah perubahan manual admin —
     * pembelian dan penjualan memperbarui stok langsung lewat repository, jadi
     * tidak akan terjurnal dua kali.
     */
    private final PenyesuaianStokService penyesuaianStokService;

    @Transactional(readOnly = true)
    public List<ProdukResponse> getAllProduk() {
        List<ProdukResponse> hasil = toResponses(produkRepository.findByDeletedFalse());
        log.debug("Fetched {} active products", hasil.size());
        return hasil;
    }

    @Transactional(readOnly = true)
    public ProdukResponse getProdukById(Long id) {
        log.debug("Fetching product id={}", id);
        Produk produk = produkRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));
        return convertToResponse(produk);
    }

    @Transactional(readOnly = true)
    public List<ProdukResponse> getProdukByStatus(StatusProduk status) {
        List<ProdukResponse> hasil = toResponses(produkRepository.findByStatusAndDeletedFalse(status));
        log.debug("Fetched {} products with status={}", hasil.size(), status);
        return hasil;
    }

    @Transactional(readOnly = true)
    public List<ProdukResponse> searchProdukByNama(String nama) {
        List<ProdukResponse> hasil = toResponses(produkRepository.findByNamaContainingIgnoreCaseAndDeletedFalse(nama));
        log.debug("Search nama={} matched {} products", nama, hasil.size());
        return hasil;
    }

    private static final List<Integer> VALID_KARATS = List.of(14, 18, 22, 24);

    private void validasiKaratEmas(Integer karat) {
        if (karat == null || !VALID_KARATS.contains(karat)) {
            throw new IllegalArgumentException("Karat emas tidak valid. Karat emas yang diperbolehkan hanya 14K, 18K, 22K, dan 24K.");
        }
    }

    @Transactional
    public ProdukResponse createProduk(ProdukRequest request) {
        validasiKaratEmas(request.getKaratEmas());

        log.info("Creating product nama={} harga={} stock={}",
                request.getNama(), request.getHarga(), request.getStock());
        
        Produk produk = Produk.builder()
                .nama(request.getNama())
                .deskripsi(kosongkanKalauBlank(request.getDeskripsi()))
                .gambar(coverDari(request))
                .kategori(request.getKategori())
                .harga(request.getHarga())
                .hargaModal(request.getHargaModal() != null ? request.getHargaModal() : java.math.BigDecimal.ZERO)
                .stock(request.getStock())
                .karatEmas(request.getKaratEmas())
                .beratGram(request.getBeratGram())
                .status(request.getStatus() != null ? request.getStatus() : StatusProduk.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .build();

        Produk savedProduk = produkRepository.save(produk);
        syncGambar(savedProduk, request);
        log.info("Product created: id={} nama={} stock={}",
                savedProduk.getId(), savedProduk.getNama(), savedProduk.getStock());

        // Stok awal adalah barang yang sudah ada di toko tapi belum pernah
        // masuk pembukuan, jadi nilainya dicatat sebagai penambahan persediaan.
        penyesuaianStokService.catat(savedProduk, 0, savedProduk.getStock(), "stok awal produk baru");

        return convertToResponse(savedProduk);
    }

    @Transactional
    public ProdukResponse updateProduk(Long id, ProdukRequest request) {
        validasiKaratEmas(request.getKaratEmas());

        log.info("Updating product id={} nama={} harga={}", id, request.getNama(), request.getHarga());
        
        Produk produk = produkRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        List<String> gambarLama = urlsGambar(produk);
        List<String> gambarBaru = normalizeGambarList(request);

        String coverBaru = gambarBaru.isEmpty() ? null : gambarBaru.get(0);

        produk.setNama(request.getNama());
        produk.setDeskripsi(kosongkanKalauBlank(request.getDeskripsi()));
        produk.setGambar(coverBaru);
        produk.setKategori(request.getKategori());
        produk.setHarga(request.getHarga());
        if (request.getHargaModal() != null) {
            produk.setHargaModal(request.getHargaModal());
        }
        // Stok tidak diubah di sini: koreksi lewat PATCH /stock (penyesuaian),
        // barang masuk lewat Akuntansi → Pembelian.
        produk.setKaratEmas(request.getKaratEmas());
        produk.setBeratGram(request.getBeratGram());
        if (request.getStatus() != null) {
            produk.setStatus(request.getStatus());
        }
        produk.setUpdatedAt(LocalDateTime.now());

        Produk updatedProduk = produkRepository.save(produk);
        syncGambar(updatedProduk, request);
        hapusGambarYangTidakDipakai(gambarLama, gambarBaru);
        log.info("Product updated: id={} nama={}", updatedProduk.getId(), updatedProduk.getNama());

        return convertToResponse(updatedProduk);
    }

    /**
     * Menghapus produk secara logis.
     *
     * Menghapus barisnya benar-benar akan gagal karena masih ditunjuk pesanan,
     * review, dan catatan penjualan — dan kalaupun berhasil, riwayat pembelian
     * pelanggan ikut rusak. Gambarnya pun sengaja dipertahankan di Cloudinary
     * karena URL-nya masih dipakai riwayat pesanan.
     */
    @Transactional
    public void deleteProduk(Long id) {
        log.info("Soft deleting product id={}", id);

        Produk produk = produkRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        produk.setDeleted(true);
        produk.setDeletedAt(LocalDateTime.now());
        produk.setStatus(StatusProduk.TIDAK_TERSEDIA);
        produk.setUpdatedAt(LocalDateTime.now());
        produkRepository.save(produk);

        // Varian ikut dinonaktifkan supaya tidak bisa dipakai lagi
        varianService.nonaktifkanSemua(id);

        // Bersihkan dari keranjang & wishlist supaya tidak bisa di-checkout
        keranjangRepository.deleteByProdukId(id);
        wishlistRepository.deleteByProdukId(id);

        log.info("Product soft deleted: id={} nama={}, variants deactivated and removed from carts/wishlists",
                id, produk.getNama());
    }

    @Transactional
    public ProdukResponse updateStatus(Long id, StatusProduk status) {
        log.info("Updating product status id={} to={}", id, status);
        
        Produk produk = produkRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        produk.setStatus(status);
        produk.setUpdatedAt(LocalDateTime.now());

        Produk updatedProduk = produkRepository.save(produk);
        log.info("Product status updated: id={} status={}", updatedProduk.getId(), updatedProduk.getStatus());
        return convertToResponse(updatedProduk);
    }

    @Transactional
    public ProdukResponse updateStock(Long id, Integer stock) {
        log.info("Updating product stock id={} to={}", id, stock);
        
        if (stock < 0) {
            log.warn("Rejected negative stock {} for product id={}", stock, id);
            throw new IllegalArgumentException("Stock tidak boleh negatif");
        }

        Produk produk = produkRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        Integer stokLama = produk.getStock();

        produk.setStock(stock);
        produk.setUpdatedAt(LocalDateTime.now());

        // Auto update status based on stock
        if (stock == 0) {
            produk.setStatus(StatusProduk.HABIS);
        } else if (produk.getStatus() == StatusProduk.HABIS) {
            produk.setStatus(StatusProduk.TERSEDIA);
        }

        Produk updatedProduk = produkRepository.save(produk);
        log.info("Product stock updated: id={} from={} to={} status={}",
                id, stokLama, stock, updatedProduk.getStatus());

        penyesuaianStokService.catat(updatedProduk, stokLama, stock, "stok disetel admin");

        // Stok kembali tersedia → kirim notifikasi ke pendaftar "beri tahu saya".
        restockNotifikasiService.cekRestock(updatedProduk);

        // Check if stock is low and send notification
        if (stock < settingService.getInt("lowStock.threshold", 5) && stock > 0) {
            log.info("Low stock: productId={} nama={} stock={}, sending notification",
                    id, produk.getNama(), stock);
            notificationService.sendLowStockNotification(updatedProduk);
        }
        
        return convertToResponse(updatedProduk);
    }

    private List<ProdukResponse> toResponses(List<Produk> products) {
        Map<Long, Integer> terjualPerProduk = soldCounts();

        List<Long> ids = products.stream().map(Produk::getId).toList();

        Map<Long, List<ProdukVarian>> varianPerProduk = ids.isEmpty()
                ? Map.of()
                : varianRepository.findByProdukIdIn(ids).stream()
                        .collect(Collectors.groupingBy(v -> v.getProduk().getId()));

        Map<Long, List<String>> gambarPerProduk = ids.isEmpty()
                ? Map.of()
                : gambarRepository.findByProdukIdIn(ids).stream()
                        .collect(Collectors.groupingBy(
                                g -> g.getProduk().getId(),
                                Collectors.collectingAndThen(Collectors.toList(), rows -> rows.stream()
                                        .sorted((a, b) -> {
                                            int urutan = Integer.compare(
                                                    a.getUrutan() != null ? a.getUrutan() : 0,
                                                    b.getUrutan() != null ? b.getUrutan() : 0);
                                            if (urutan != 0) {
                                                return urutan;
                                            }
                                            return Long.compare(
                                                    a.getId() != null ? a.getId() : 0L,
                                                    b.getId() != null ? b.getId() : 0L);
                                        })
                                        .map(ProdukGambar::getUrl)
                                        .toList())));

        return products.stream()
                .map(produk -> convertToResponse(
                        produk,
                        terjualPerProduk.getOrDefault(produk.getId(), 0),
                        varianPerProduk.getOrDefault(produk.getId(), List.of()),
                        gambarPerProduk.getOrDefault(produk.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private Map<Long, Integer> soldCounts() {
        Map<Long, Integer> terjualPerProduk = new HashMap<>();
        for (Object[] row : produkTerjualRepository.sumQtyGroupByProduk()) {
            terjualPerProduk.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        return terjualPerProduk;
    }

    private ProdukResponse convertToResponse(Produk produk) {
        Integer terjual = produkTerjualRepository.getTotalTerjualByProduk(produk.getId());
        List<ProdukVarian> varianList = varianRepository.findByProdukIdOrderByIdAsc(produk.getId());
        List<String> gambarList = urlsGambar(produk);
        return convertToResponse(produk, terjual != null ? terjual : 0, varianList, gambarList);
    }

    /**
     * Katalog produk terbuka untuk umum, sedangkan harga modal adalah rahasia dagang:
     * dari situ margin toko bisa dihitung siapa pun. Karena itu nilainya hanya diisi
     * bila yang meminta seorang admin — disaring di satu tempat ini supaya tidak ada
     * endpoint yang tanpa sengaja membocorkannya.
     */
    private boolean pemintaAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private ProdukResponse convertToResponse(Produk produk, Integer terjual) {
        return convertToResponse(produk, terjual, List.of(), urlsGambar(produk));
    }

    private ProdukResponse convertToResponse(Produk produk, Integer terjual, List<ProdukVarian> varianList) {
        return convertToResponse(produk, terjual, varianList, urlsGambar(produk));
    }

    private ProdukResponse convertToResponse(
            Produk produk, Integer terjual, List<ProdukVarian> varianList, List<String> gambarList) {
        boolean admin = pemintaAdmin();
        List<ProdukVarianResponse> varian = varianList.stream()
                .filter(v -> admin || Boolean.TRUE.equals(v.getAktif()))
                .map(v -> ProdukVarianResponse.from(v, admin))
                .toList();

        List<String> foto = gambarList == null || gambarList.isEmpty()
                ? urlsDariCover(produk)
                : gambarList;

        String cover = foto.isEmpty() ? produk.getGambar() : foto.get(0);

        return ProdukResponse.builder()
                .id(produk.getId())
                .nama(produk.getNama())
                .deskripsi(produk.getDeskripsi())
                .gambar(cover)
                .gambarList(foto)
                .kategori(produk.getKategori())
                .harga(produk.getHarga())
                .hargaModal(admin ? produk.getHargaModal() : null)
                .stock(produk.getStock())
                .terjual(terjual)
                .karatEmas(produk.getKaratEmas())
                .beratGram(produk.getBeratGram())
                .status(produk.getStatus())
                .varian(varian)
                .createdAt(produk.getCreatedAt())
                .updatedAt(produk.getUpdatedAt())
                .build();
    }

    static final int MAKS_GAMBAR = 8;

    private void syncGambar(Produk produk, ProdukRequest request) {
        List<String> urls = normalizeGambarList(request);
        gambarRepository.deleteByProdukId(produk.getId());
        gambarRepository.flush();

        int urutan = 0;
        for (String url : urls) {
            gambarRepository.save(ProdukGambar.builder()
                    .produk(produk)
                    .url(url)
                    .urutan(urutan++)
                    .build());
        }

        produk.setGambar(urls.isEmpty() ? null : urls.get(0));
        produkRepository.save(produk);
    }

    private void hapusGambarYangTidakDipakai(List<String> lama, List<String> baru) {
        List<String> buang = lama.stream()
                .filter(url -> url != null && !url.isBlank() && !baru.contains(url))
                .toList();
        if (buang.isEmpty()) {
            return;
        }

        Runnable hapus = () -> {
            for (String url : buang) {
                hapusCloudinaryKalauAman(url);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    hapus.run();
                }
            });
            return;
        }

        hapus.run();
    }

    /**
     * URL yang masih tersimpan di item pesanan tidak dihapus dari Cloudinary —
     * riwayat belanja pembeli masih menampilkan foto itu.
     */
    private void hapusCloudinaryKalauAman(String url) {
        if (pesananItemRepository.existsByGambarProduk(url)) {
            log.debug("Keeping Cloudinary image still referenced by order history url={}", url);
            return;
        }
        String publicId = cloudinaryService.extractPublicId(url);
        if (publicId == null) {
            return;
        }
        log.debug("Removing unused Cloudinary image publicId={}", publicId);
        cloudinaryService.deleteImage(publicId);
    }

    private List<String> urlsGambar(Produk produk) {
        List<String> fromRows = gambarRepository.findByProdukIdOrderByUrutanAscIdAsc(produk.getId())
                .stream()
                .map(ProdukGambar::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        return fromRows.isEmpty() ? urlsDariCover(produk) : fromRows;
    }

    private static List<String> urlsDariCover(Produk produk) {
        if (produk.getGambar() == null || produk.getGambar().isBlank()) {
            return List.of();
        }
        return List.of(produk.getGambar());
    }

    private static String coverDari(ProdukRequest request) {
        List<String> urls = normalizeGambarList(request);
        return urls.isEmpty() ? null : urls.get(0);
    }

    static List<String> normalizeGambarList(ProdukRequest request) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (request.getGambarList() != null) {
            for (String raw : request.getGambarList()) {
                String url = urlValid(raw);
                if (url != null) {
                    unique.add(url);
                }
            }
        }
        if (unique.isEmpty()) {
            String cover = urlValid(request.getGambar());
            if (cover != null) {
                unique.add(cover);
            }
        }
        List<String> urls = new ArrayList<>(unique);
        if (urls.size() > MAKS_GAMBAR) {
            return new ArrayList<>(urls.subList(0, MAKS_GAMBAR));
        }
        return urls;
    }

    private static String urlValid(String raw) {
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null;
        }
        return url;
    }

    private static String kosongkanKalauBlank(String nilai) {
        if (nilai == null) {
            return null;
        }
        String trimmed = nilai.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
