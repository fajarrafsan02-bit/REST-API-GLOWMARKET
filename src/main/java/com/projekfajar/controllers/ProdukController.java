package com.projekfajar.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.projekfajar.DTO.ProdukRequest;
import com.projekfajar.DTO.ProdukResponse;
import com.projekfajar.models.StatusProduk;
import com.projekfajar.services.CloudinaryService;
import com.projekfajar.services.ProdukService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/produk")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ProdukController {
    private static final Logger logger = LoggerFactory.getLogger(ProdukController.class);
    private final ProdukService produkService;
    private final CloudinaryService cloudinaryService;

    @Value("${server.port:8080}")
    private String serverPort;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProduk() {
        try {
            logger.info("Request to get all products");
            List<ProdukResponse> produkList = produkService.getAllProduk();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk berhasil diambil",
                    "data", produkList));
        } catch (Exception e) {
            logger.error("Error getting all products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mengambil data produk"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProdukById(@PathVariable Long id) {
        try {
            logger.info("Request to get product with id: {}", id);
            ProdukResponse produk = produkService.getProdukById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk berhasil diambil",
                    "data", produk));
        } catch (Exception e) {
            logger.error("Error getting product by id: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getProdukByStatus(@PathVariable StatusProduk status) {
        try {
            logger.info("Request to get products with status: {}", status);
            List<ProdukResponse> produkList = produkService.getProdukByStatus(status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk berhasil diambil",
                    "data", produkList));
        } catch (Exception e) {
            logger.error("Error getting products by status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mengambil data produk"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProduk(@RequestParam String nama) {
        try {
            logger.info("Request to search products with name: {}", nama);
            List<ProdukResponse> produkList = produkService.searchProdukByNama(nama);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pencarian produk berhasil",
                    "data", produkList));
        } catch (Exception e) {
            logger.error("Error searching products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mencari produk"));
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Request to upload image to Cloudinary: {}", file.getOriginalFilename());
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "File tidak boleh kosong"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "File harus berupa gambar"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Ukuran file maksimal 5MB"));
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file);
            String imageUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Gambar berhasil diupload ke Cloudinary",
                    "imageUrl", imageUrl,
                    "publicId", publicId));
        } catch (Exception e) {
            logger.error("Error uploading image to Cloudinary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal upload gambar: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduk(@Valid @RequestBody ProdukRequest request) {
        try {
            logger.info("Request to create new product: {}", request.getNama());
            
            // Validate that gambar is a valid Cloudinary URL if provided
            if (request.getGambar() != null && !request.getGambar().isEmpty()) {
                if (!request.getGambar().startsWith("http://") && !request.getGambar().startsWith("https://")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "success", false,
                                    "message", "Gambar harus berupa URL yang valid. Gunakan endpoint /api/produk/upload-image untuk upload gambar terlebih dahulu."));
                }
            }
            
            ProdukResponse produk = produkService.createProduk(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Produk berhasil dibuat",
                            "data", produk));
        } catch (Exception e) {
            logger.error("Error creating product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal membuat produk"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduk(
            @PathVariable Long id,
            @Valid @RequestBody ProdukRequest request) {
        try {
            logger.info("Request to update product with id: {}", id);
            ProdukResponse produk = produkService.updateProduk(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produk berhasil diupdate",
                    "data", produk));
        } catch (Exception e) {
            logger.error("Error updating product: {}", e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduk(@PathVariable Long id) {
        try {
            logger.info("Request to delete product with id: {}", id);
            produkService.deleteProduk(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produk berhasil dihapus"));
        } catch (Exception e) {
            logger.error("Error deleting product: {}", e.getMessage());
            throw e;
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            logger.info("Request to update status for product id: {}", id);
            StatusProduk status = StatusProduk.valueOf(request.get("status"));
            ProdukResponse produk = produkService.updateStatus(id, status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status produk berhasil diupdate",
                    "data", produk));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Status tidak valid"));
        } catch (Exception e) {
            logger.error("Error updating product status: {}", e.getMessage());
            throw e;
        }
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        try {
            logger.info("Request to update stock for product id: {}", id);
            Integer stock = request.get("stock");
            if (stock == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Stock tidak boleh kosong"));
            }
            ProdukResponse produk = produkService.updateStock(id, stock);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Stock produk berhasil diupdate",
                    "data", produk));
        } catch (Exception e) {
            logger.error("Error updating product stock: {}", e.getMessage());
            throw e;
        }
    }
}
