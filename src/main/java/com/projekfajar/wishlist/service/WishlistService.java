package com.projekfajar.wishlist.service;

import com.projekfajar.exception.UnauthorizedAccessException;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.produk.dto.ProdukResponse;
import com.projekfajar.wishlist.dto.WishlistResponse;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukGambar;
import com.projekfajar.user.model.User;
import com.projekfajar.wishlist.model.Wishlist;
import com.projekfajar.produk.repository.ProdukGambarRepository;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.wishlist.repository.WishlistRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProdukRepository produkRepository;
    private final ProdukGambarRepository gambarRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlistByUser(Long userId) {
        log.info("Fetching wishlist for user: {}", userId);
        List<Wishlist> items = wishlistRepository.findByUserId(userId);
        Map<Long, List<String>> gambarPerProduk = loadGambar(
                items.stream().map(w -> w.getProduk().getId()).toList());
        return items.stream()
                .map(item -> convertToResponse(
                        item,
                        gambarPerProduk.getOrDefault(item.getProduk().getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long produkId) {
        log.info("Adding product {} to wishlist for user {}", produkId, userId);

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProdukId(userId, produkId)) {
            throw new BusinessException("Produk sudah ada di wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        Produk produk = produkRepository.findByIdAndDeletedFalse(produkId)
                .orElseThrow(() -> new ProdukNotFoundException("Produk tidak ditemukan"));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .produk(produk)
                .createdAt(LocalDateTime.now())
                .build();

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        log.info("Product added to wishlist successfully");
        return convertToResponse(savedWishlist, urlsGambar(produk.getId(), produk.getGambar()));
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long wishlistId) {
        log.info("Removing wishlist item {} for user {}", wishlistId, userId);

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Item wishlist tidak ditemukan"));

        if (!wishlist.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Anda tidak memiliki akses ke data ini");
        }

        wishlistRepository.delete(wishlist);
        log.info("Wishlist item removed successfully");
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long produkId) {
        return wishlistRepository.existsByUserIdAndProdukId(userId, produkId);
    }

    private Map<Long, List<String>> loadGambar(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }

        return gambarRepository.findByProdukIdIn(ids).stream()
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
                                .filter(url -> url != null && !url.isBlank())
                                .toList())));
    }

    private List<String> urlsGambar(Long produkId, String cover) {
        List<String> fromRows = gambarRepository.findByProdukIdOrderByUrutanAscIdAsc(produkId)
                .stream()
                .map(ProdukGambar::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        return fromRows.isEmpty() ? urlsDariCover(cover) : fromRows;
    }

    private static List<String> urlsDariCover(String cover) {
        if (cover == null || cover.isBlank()) {
            return List.of();
        }
        return List.of(cover);
    }

    private WishlistResponse convertToResponse(Wishlist wishlist, List<String> gambarList) {
        Produk produk = wishlist.getProduk();
        List<String> foto = gambarList == null || gambarList.isEmpty()
                ? urlsDariCover(produk.getGambar())
                : gambarList;
        String cover = foto.isEmpty() ? produk.getGambar() : foto.get(0);

        ProdukResponse produkResponse = ProdukResponse.builder()
                .id(produk.getId())
                .nama(produk.getNama())
                .deskripsi(produk.getDeskripsi())
                .gambar(cover)
                .gambarList(foto)
                .kategori(produk.getKategori())
                .harga(produk.getHarga())
                .stock(produk.getStock())
                .karatEmas(produk.getKaratEmas())
                .beratGram(produk.getBeratGram())
                .status(produk.getStatus())
                .createdAt(produk.getCreatedAt())
                .updatedAt(produk.getUpdatedAt())
                .build();

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .produkId(produk.getId())
                .produk(produkResponse)
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}
