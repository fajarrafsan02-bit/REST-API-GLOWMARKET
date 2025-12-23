package com.projekfajar.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.DTO.ProdukRequest;
import com.projekfajar.DTO.ProdukResponse;
import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.models.Produk;
import com.projekfajar.models.StatusProduk;
import com.projekfajar.models.TerjualProduk;
import com.projekfajar.repository.ProdukRepository;
import com.projekfajar.repository.TerjualProdukRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdukService {
    private static final Logger logger = LoggerFactory.getLogger(ProdukService.class);
    private final ProdukRepository produkRepository;
    private final CloudinaryService cloudinaryService;
    private final TerjualProdukRepository terjualProdukRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ProdukResponse> getAllProduk() {
        logger.info("Fetching all products");
        return produkRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProdukResponse getProdukById(Long id) {
        logger.info("Fetching product with id: {}", id);
        Produk produk = produkRepository.findById(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));
        return convertToResponse(produk);
    }

    @Transactional(readOnly = true)
    public List<ProdukResponse> getProdukByStatus(StatusProduk status) {
        logger.info("Fetching products with status: {}", status);
        return produkRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProdukResponse> searchProdukByNama(String nama) {
        logger.info("Searching products with name containing: {}", nama);
        return produkRepository.findByNamaContainingIgnoreCase(nama).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProdukResponse createProduk(ProdukRequest request) {
        logger.info("Creating new product: {}", request.getNama());
        
        Produk produk = Produk.builder()
                .nama(request.getNama())
                .gambar(request.getGambar())
                .harga(request.getHarga())
                .stock(request.getStock())
                .karatEmas(request.getKaratEmas())
                .beratGram(request.getBeratGram())
                .status(request.getStatus() != null ? request.getStatus() : StatusProduk.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .build();

        Produk savedProduk = produkRepository.save(produk);
        logger.info("Product created successfully with id: {}", savedProduk.getId());
        
        // Auto-create terjual_produk entry with initial value 0
        TerjualProduk terjualProduk = TerjualProduk.builder()
                .produk(savedProduk)
                .terjual(0)
                .build();
        terjualProdukRepository.save(terjualProduk);
        logger.info("TerjualProduk entry created for product id: {}", savedProduk.getId());
        
        return convertToResponse(savedProduk);
    }

    @Transactional
    public ProdukResponse updateProduk(Long id, ProdukRequest request) {
        logger.info("Updating product with id: {}", id);
        
        Produk produk = produkRepository.findById(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        // Delete old image from Cloudinary if new image is different
        if (request.getGambar() != null && !request.getGambar().equals(produk.getGambar())) {
            String oldPublicId = cloudinaryService.extractPublicId(produk.getGambar());
            if (oldPublicId != null) {
                cloudinaryService.deleteImage(oldPublicId);
            }
        }

        produk.setNama(request.getNama());
        produk.setGambar(request.getGambar());
        produk.setHarga(request.getHarga());
        produk.setStock(request.getStock());
        produk.setKaratEmas(request.getKaratEmas());
        produk.setBeratGram(request.getBeratGram());
        if (request.getStatus() != null) {
            produk.setStatus(request.getStatus());
        }
        produk.setUpdatedAt(LocalDateTime.now());

        Produk updatedProduk = produkRepository.save(produk);
        logger.info("Product updated successfully with id: {}", updatedProduk.getId());
        return convertToResponse(updatedProduk);
    }

    @Transactional
    public void deleteProduk(Long id) {
        logger.info("Deleting product with id: {}", id);
        
        Produk produk = produkRepository.findById(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        // Delete image from Cloudinary
        String publicId = cloudinaryService.extractPublicId(produk.getGambar());
        if (publicId != null) {
            cloudinaryService.deleteImage(publicId);
        }
        
        // Delete terjual_produk entry
        terjualProdukRepository.findByProdukId(id).ifPresent(terjualProdukRepository::delete);

        produkRepository.delete(produk);
        logger.info("Product deleted successfully with id: {}", id);
    }

    @Transactional
    public ProdukResponse updateStatus(Long id, StatusProduk status) {
        logger.info("Updating product status with id: {} to {}", id, status);
        
        Produk produk = produkRepository.findById(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        produk.setStatus(status);
        produk.setUpdatedAt(LocalDateTime.now());

        Produk updatedProduk = produkRepository.save(produk);
        logger.info("Product status updated successfully");
        return convertToResponse(updatedProduk);
    }

    @Transactional
    public ProdukResponse updateStock(Long id, Integer stock) {
        logger.info("Updating product stock with id: {} to {}", id, stock);
        
        if (stock < 0) {
            throw new IllegalArgumentException("Stock tidak boleh negatif");
        }

        Produk produk = produkRepository.findById(id)
                .orElseThrow(() -> new ProdukNotFoundException("Produk dengan ID " + id + " tidak ditemukan"));

        produk.setStock(stock);
        produk.setUpdatedAt(LocalDateTime.now());

        // Auto update status based on stock
        if (stock == 0) {
            produk.setStatus(StatusProduk.HABIS);
        } else if (produk.getStatus() == StatusProduk.HABIS) {
            produk.setStatus(StatusProduk.TERSEDIA);
        }

        Produk updatedProduk = produkRepository.save(produk);
        logger.info("Product stock updated successfully");
        
        // Check if stock is low and send notification
        if (stock < 5 && stock > 0) {
            logger.info("Stock for product {} is low ({}), sending notification", produk.getNama(), stock);
            notificationService.sendLowStockNotification(updatedProduk);
        }
        
        return convertToResponse(updatedProduk);
    }

    private ProdukResponse convertToResponse(Produk produk) {
        return ProdukResponse.builder()
                .id(produk.getId())
                .nama(produk.getNama())
                .gambar(produk.getGambar())
                .harga(produk.getHarga())
                .stock(produk.getStock())
                .karatEmas(produk.getKaratEmas())
                .beratGram(produk.getBeratGram())
                .status(produk.getStatus())
                .createdAt(produk.getCreatedAt())
                .updatedAt(produk.getUpdatedAt())
                .build();
    }
}
