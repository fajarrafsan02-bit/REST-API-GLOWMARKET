package com.projekfajar.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.DTO.KeranjangRequest;
import com.projekfajar.DTO.KeranjangResponse;
import com.projekfajar.DTO.ProdukResponse;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.models.Keranjang;
import com.projekfajar.models.Produk;
import com.projekfajar.models.User;
import com.projekfajar.repository.KeranjangRepository;
import com.projekfajar.repository.ProdukRepository;
import com.projekfajar.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KeranjangService {
    private static final Logger logger = LoggerFactory.getLogger(KeranjangService.class);
    
    private final KeranjangRepository keranjangRepository;
    private final ProdukRepository produkRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<KeranjangResponse> getKeranjangByUser(Long userId) {
        logger.info("Fetching cart for user: {}", userId);
        return keranjangRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public KeranjangResponse addToKeranjang(Long userId, KeranjangRequest request) {
        logger.info("Adding product {} to cart for user {}", request.getProdukId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        Produk produk = produkRepository.findById(request.getProdukId())
                .orElseThrow(() -> new ProdukNotFoundException("Produk tidak ditemukan"));

        // Check if product already in cart
        Keranjang keranjang = keranjangRepository.findByUserIdAndProdukId(userId, request.getProdukId())
                .orElse(null);

        if (keranjang != null) {
            // Update quantity
            keranjang.setQuantity(keranjang.getQuantity() + request.getQuantity());
            keranjang.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new cart item
            LocalDateTime now = LocalDateTime.now();
            keranjang = Keranjang.builder()
                    .user(user)
                    .produk(produk)
                    .quantity(request.getQuantity())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        Keranjang savedKeranjang = keranjangRepository.save(keranjang);
        logger.info("Product added to cart successfully");
        return convertToResponse(savedKeranjang);
    }

    @Transactional
    public KeranjangResponse updateQuantity(Long userId, Long keranjangId, Integer quantity) {
        logger.info("Updating cart item {} quantity to {}", keranjangId, quantity);

        Keranjang keranjang = keranjangRepository.findById(keranjangId)
                .orElseThrow(() -> new RuntimeException("Item keranjang tidak ditemukan"));

        if (!keranjang.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity harus lebih dari 0");
        }

        keranjang.setQuantity(quantity);
        keranjang.setUpdatedAt(LocalDateTime.now());

        Keranjang updatedKeranjang = keranjangRepository.save(keranjang);
        logger.info("Cart item updated successfully");
        return convertToResponse(updatedKeranjang);
    }

    @Transactional
    public void removeFromKeranjang(Long userId, Long keranjangId) {
        logger.info("Removing cart item {} for user {}", keranjangId, userId);

        Keranjang keranjang = keranjangRepository.findById(keranjangId)
                .orElseThrow(() -> new RuntimeException("Item keranjang tidak ditemukan"));

        if (!keranjang.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        keranjangRepository.delete(keranjang);
        logger.info("Cart item removed successfully");
    }

    @Transactional
    public void clearKeranjang(Long userId) {
        logger.info("Clearing cart for user {}", userId);
        keranjangRepository.deleteByUserId(userId);
        logger.info("Cart cleared successfully");
    }

    private KeranjangResponse convertToResponse(Keranjang keranjang) {
        ProdukResponse produkResponse = ProdukResponse.builder()
                .id(keranjang.getProduk().getId())
                .nama(keranjang.getProduk().getNama())
                .gambar(keranjang.getProduk().getGambar())
                .harga(keranjang.getProduk().getHarga())
                .stock(keranjang.getProduk().getStock())
                .karatEmas(keranjang.getProduk().getKaratEmas())
                .status(keranjang.getProduk().getStatus())
                .createdAt(keranjang.getProduk().getCreatedAt())
                .updatedAt(keranjang.getProduk().getUpdatedAt())
                .build();

        return KeranjangResponse.builder()
                .id(keranjang.getId())
                .userId(keranjang.getUser().getId())
                .produk(produkResponse)
                .quantity(keranjang.getQuantity())
                .subtotal(keranjang.getProduk().getHarga() * keranjang.getQuantity())
                .createdAt(keranjang.getCreatedAt())
                .updatedAt(keranjang.getUpdatedAt())
                .build();
    }
}
