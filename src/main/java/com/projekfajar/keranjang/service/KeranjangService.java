package com.projekfajar.keranjang.service;

import com.projekfajar.exception.UnauthorizedAccessException;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.keranjang.dto.KeranjangRequest;
import com.projekfajar.keranjang.dto.KeranjangResponse;
import com.projekfajar.produk.dto.ProdukResponse;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.keranjang.model.Keranjang;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukVarian;
import com.projekfajar.produk.repository.ProdukVarianRepository;
import com.projekfajar.user.model.User;
import com.projekfajar.keranjang.repository.KeranjangRepository;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeranjangService {
    
    private final KeranjangRepository keranjangRepository;
    private final ProdukRepository produkRepository;
    private final ProdukVarianRepository varianRepository;
    private final UserRepository userRepository;

    /**
     * Stok divalidasi sejak keranjang, bukan menunggu sampai proses pembayaran,
     * supaya pembeli tahu lebih awal kalau jumlahnya melebihi stok.
     */
    private void validateStock(Produk produk, int quantity) {
        validateStock(produk, null, quantity);
    }

    private void validateStock(Produk produk, ProdukVarian varian, int quantity) {
        int stock = varian != null
                ? (varian.getStock() != null ? varian.getStock() : 0)
                : (produk.getStock() != null ? produk.getStock() : 0);
        String nama = varian != null ? produk.getNama() + " (" + varian.getNama() + ")" : produk.getNama();

        if (stock <= 0) {
            throw new BusinessException("Stok " + nama + " habis");
        }

        if (quantity > stock) {
            throw new BusinessException("Stok " + nama + " tersisa " + stock);
        }
    }

    /**
     * Memuat varian yang diminta dan memastikan dia milik produk ini dan masih
     * aktif. Variant yang sudah dinonaktifkan tidak boleh dipakai keranjang.
     */
    private ProdukVarian resolveVarian(Produk produk, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProdukVarian varian = varianRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException("Varian tidak ditemukan"));
        if (!varian.getProduk().getId().equals(produk.getId())) {
            throw new BusinessException("Varian tidak cocok dengan produk");
        }
        if (!Boolean.TRUE.equals(varian.getAktif())) {
            throw new BusinessException("Varian " + varian.getNama() + " tidak tersedia");
        }
        return varian;
    }

    @Transactional(readOnly = true)
    public List<KeranjangResponse> getKeranjangByUser(Long userId) {
        log.info("Fetching cart for user: {}", userId);
        return keranjangRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public KeranjangResponse addToKeranjang(Long userId, KeranjangRequest request) {
        log.info("Adding product {} to cart for user {}", request.getProdukId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        Produk produk = produkRepository.findByIdAndDeletedFalse(request.getProdukId())
                .orElseThrow(() -> new ProdukNotFoundException("Produk tidak ditemukan"));

        ProdukVarian varian = resolveVarian(produk, request.getVariantId());

        // Check if product (and its variant) already in cart
        Keranjang keranjang = request.getVariantId() != null
                ? keranjangRepository
                        .findByUserIdAndProdukIdAndVariantId(userId, request.getProdukId(), request.getVariantId())
                        .orElse(null)
                : keranjangRepository.findByUserIdAndProdukIdAndVariantIsNull(userId, request.getProdukId())
                        .orElse(null);

        if (keranjang != null) {
            // Update quantity
            int newQuantity = keranjang.getQuantity() + request.getQuantity();
            validateStock(produk, keranjang.getVariant(), newQuantity);
            keranjang.setQuantity(newQuantity);
            keranjang.setUpdatedAt(LocalDateTime.now());
        } else {
            validateStock(produk, varian, request.getQuantity());
            // Create new cart item
            LocalDateTime now = LocalDateTime.now();
            keranjang = Keranjang.builder()
                    .user(user)
                    .produk(produk)
                    .variant(varian)
                    .quantity(request.getQuantity())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        Keranjang savedKeranjang = keranjangRepository.save(keranjang);
        log.info("Product added to cart successfully");
        return convertToResponse(savedKeranjang);
    }

    @Transactional
    public KeranjangResponse updateQuantity(Long userId, Long keranjangId, Integer quantity) {
        log.info("Updating cart item {} quantity to {}", keranjangId, quantity);

        Keranjang keranjang = keranjangRepository.findById(keranjangId)
                .orElseThrow(() -> new ResourceNotFoundException("Item keranjang tidak ditemukan"));

        if (!keranjang.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Anda tidak memiliki akses ke data ini");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity harus lebih dari 0");
        }

        validateStock(keranjang.getProduk(), keranjang.getVariant(), quantity);

        keranjang.setQuantity(quantity);
        keranjang.setUpdatedAt(LocalDateTime.now());

        Keranjang updatedKeranjang = keranjangRepository.save(keranjang);
        log.info("Cart item updated successfully");
        return convertToResponse(updatedKeranjang);
    }

    @Transactional
    public void removeFromKeranjang(Long userId, Long keranjangId) {
        log.info("Removing cart item {} for user {}", keranjangId, userId);

        Keranjang keranjang = keranjangRepository.findById(keranjangId)
                .orElseThrow(() -> new ResourceNotFoundException("Item keranjang tidak ditemukan"));

        if (!keranjang.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Anda tidak memiliki akses ke data ini");
        }

        keranjangRepository.delete(keranjang);
        log.info("Cart item removed successfully");
    }

    @Transactional
    public void clearKeranjang(Long userId) {
        log.info("Clearing cart for user {}", userId);
        keranjangRepository.deleteByUserId(userId);
        log.info("Cart cleared successfully");
    }

    private KeranjangResponse convertToResponse(Keranjang keranjang) {
        ProdukVarian varian = keranjang.getVariant();
        BigDecimal harga = varian != null ? varian.getHarga() : keranjang.getProduk().getHarga();
        String namaVariant = varian != null ? varian.getNama() : null;

        ProdukResponse produkResponse = ProdukResponse.builder()
                .id(keranjang.getProduk().getId())
                .nama(keranjang.getProduk().getNama())
                .deskripsi(keranjang.getProduk().getDeskripsi())
                .gambar(keranjang.getProduk().getGambar())
                .harga(harga)
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
                .variantId(varian != null ? varian.getId() : null)
                .namaVariant(namaVariant)
                .quantity(keranjang.getQuantity())
                .subtotal(harga.multiply(BigDecimal.valueOf(keranjang.getQuantity())))
                .createdAt(keranjang.getCreatedAt())
                .updatedAt(keranjang.getUpdatedAt())
                .build();
    }
}
