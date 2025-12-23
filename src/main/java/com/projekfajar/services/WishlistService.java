package com.projekfajar.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.DTO.ProdukResponse;
import com.projekfajar.DTO.WishlistResponse;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.models.Produk;
import com.projekfajar.models.User;
import com.projekfajar.models.Wishlist;
import com.projekfajar.repository.ProdukRepository;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.repository.WishlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);

    private final WishlistRepository wishlistRepository;
    private final ProdukRepository produkRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlistByUser(Long userId) {
        logger.info("Fetching wishlist for user: {}", userId);
        return wishlistRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long produkId) {
        logger.info("Adding product {} to wishlist for user {}", produkId, userId);

        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProdukId(userId, produkId)) {
            throw new RuntimeException("Produk sudah ada di wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        Produk produk = produkRepository.findById(produkId)
                .orElseThrow(() -> new ProdukNotFoundException("Produk tidak ditemukan"));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .produk(produk)
                .createdAt(LocalDateTime.now())
                .build();

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        logger.info("Product added to wishlist successfully");
        return convertToResponse(savedWishlist);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long wishlistId) {
        logger.info("Removing wishlist item {} for user {}", wishlistId, userId);

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new RuntimeException("Item wishlist tidak ditemukan"));

        if (!wishlist.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        wishlistRepository.delete(wishlist);
        logger.info("Wishlist item removed successfully");
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long produkId) {
        return wishlistRepository.existsByUserIdAndProdukId(userId, produkId);
    }

    private WishlistResponse convertToResponse(Wishlist wishlist) {
        ProdukResponse produkResponse = ProdukResponse.builder()
                .id(wishlist.getProduk().getId())
                .nama(wishlist.getProduk().getNama())
                .gambar(wishlist.getProduk().getGambar())
                .harga(wishlist.getProduk().getHarga())
                .stock(wishlist.getProduk().getStock())
                .karatEmas(wishlist.getProduk().getKaratEmas())
                .status(wishlist.getProduk().getStatus())
                .createdAt(wishlist.getProduk().getCreatedAt())
                .updatedAt(wishlist.getProduk().getUpdatedAt())
                .build();

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .produkId(wishlist.getProduk().getId())
                .produk(produkResponse)
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}