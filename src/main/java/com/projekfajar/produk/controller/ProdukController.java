package com.projekfajar.produk.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

import com.projekfajar.exception.UnauthorizedAccessException;
import com.projekfajar.produk.dto.ProdukRequest;
import com.projekfajar.produk.dto.ProdukResponse;
import com.projekfajar.produk.dto.ProdukVarianRequest;
import com.projekfajar.produk.dto.ProdukVarianResponse;
import com.projekfajar.produk.model.StatusProduk;
import com.projekfajar.produk.service.CloudinaryService;
import com.projekfajar.produk.service.ProdukService;
import com.projekfajar.produk.service.ProdukVarianService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/produk")
@RequiredArgsConstructor
@Slf4j
public class ProdukController {
    private final ProdukService produkService;
    private final ProdukVarianService varianService;
    private final CloudinaryService cloudinaryService;
    private final SecurityUtils securityUtils;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Semua endpoint yang mengubah data produk hanya boleh diakses admin.
     * Ini lapis kedua setelah aturan hasRole("ADMIN") di SecurityConfig.
     */
    private void requireAdmin(Authentication authentication) {
        User user = securityUtils.getCurrentUser(authentication);
        if (!securityUtils.isAdmin(user)) {
            log.warn("Non-admin access attempt to product management endpoint by userId={}",
                    user != null ? user.getId() : null);
            throw new UnauthorizedAccessException("Hanya admin yang dapat mengubah data produk");
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProduk() {
        try {
            log.info("GET /api/produk");
            List<ProdukResponse> produkList = produkService.getAllProduk();
            log.info("GET /api/produk returned {} products", produkList.size());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk berhasil diambil",
                    "data", produkList));
        } catch (Exception e) {
            log.error("Failed to fetch all products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mengambil data produk"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProdukById(@PathVariable Long id) {
        try {
            log.info("GET /api/produk/{}", id);
            ProdukResponse produk = produkService.getProdukById(id);
            log.info("Product returned: id={} name={}", id, produk.getNama());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk berhasil diambil",
                    "data", produk));
        } catch (Exception e) {
            log.error("Failed to fetch product id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getProdukByStatus(@PathVariable StatusProduk status) {
        try {
            log.info("GET /api/produk/status/{}", status);
            List<ProdukResponse> produkList = produkService.getProdukByStatus(status);
            log.info("Found {} products with status={}", produkList.size(), status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk berhasil diambil",
                    "data", produkList));
        } catch (Exception e) {
            log.error("Failed to fetch products by status={}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mengambil data produk"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProduk(@RequestParam String nama) {
        try {
            log.info("GET /api/produk/search nama={}", nama);
            List<ProdukResponse> produkList = produkService.searchProdukByNama(nama);
            log.info("Search nama={} returned {} products", nama, produkList.size());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pencarian produk berhasil",
                    "data", produkList));
        } catch (Exception e) {
            log.error("Failed to search products nama={}: {}", nama, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mencari produk"));
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("POST /api/produk/upload-image filename={} size={} bytes contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            // Validate file
            if (file.isEmpty()) {
                log.warn("Upload rejected: empty file filename={}", file.getOriginalFilename());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "File tidak boleh kosong"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Upload rejected: invalid content type={} filename={}",
                        contentType, file.getOriginalFilename());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "File harus berupa gambar"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                log.warn("Upload rejected: file too large filename={} size={} bytes",
                        file.getOriginalFilename(), file.getSize());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Ukuran file maksimal 5MB"));
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file);
            String imageUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("Image uploaded: filename={} publicId={} url={}",
                    file.getOriginalFilename(), publicId, imageUrl);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Gambar berhasil diupload ke Cloudinary",
                    "imageUrl", imageUrl,
                    "publicId", publicId));
        } catch (Exception e) {
            log.error("Failed to upload image filename={}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal upload gambar: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduk(
            @Valid @RequestBody ProdukRequest request,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("POST /api/produk nama={} harga={} stock={}",
                    request.getNama(), request.getHarga(), request.getStock());

            if (gambarRequestTidakValid(request)) {
                log.warn("Create product rejected: invalid image URL for nama={}", request.getNama());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Gambar harus berupa URL yang valid. Gunakan endpoint /api/produk/upload-image untuk upload gambar terlebih dahulu."));
            }
            
            ProdukResponse produk = produkService.createProduk(request);
            log.info("Product created: id={} nama={}", produk.getId(), produk.getNama());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Produk berhasil dibuat",
                            "data", produk));
        } catch (Exception e) {
            log.error("Failed to create product nama={}: {}", request.getNama(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal membuat produk"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduk(
            @PathVariable Long id,
            @Valid @RequestBody ProdukRequest request,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("PUT /api/produk/{} nama={} harga={}", id, request.getNama(), request.getHarga());

            if (gambarRequestTidakValid(request)) {
                log.warn("Update product rejected: invalid image URL for id={}", id);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Gambar harus berupa URL yang valid. Gunakan endpoint /api/produk/upload-image untuk upload gambar terlebih dahulu."));
            }

            ProdukResponse produk = produkService.updateProduk(id, request);
            log.info("Product updated: id={} nama={}", produk.getId(), produk.getNama());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produk berhasil diupdate",
                    "data", produk));
        } catch (Exception e) {
            log.error("Failed to update product id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduk(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("DELETE /api/produk/{}", id);
            produkService.deleteProduk(id);
            log.info("Product deleted: id={}", id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produk berhasil dihapus"));
        } catch (Exception e) {
            log.error("Failed to delete product id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("PATCH /api/produk/{}/status to={}", id, request.get("status"));
            StatusProduk status = StatusProduk.valueOf(request.get("status"));
            ProdukResponse produk = produkService.updateStatus(id, status);
            log.info("Product status updated: id={} status={}", id, produk.getStatus());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status produk berhasil diupdate",
                    "data", produk));
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value '{}' for product id={}: {}",
                    request.get("status"), id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Status tidak valid"));
        } catch (Exception e) {
            log.error("Failed to update status for product id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            Integer stock = request.get("stock");
            log.info("PATCH /api/produk/{}/stock to={}", id, stock);
            if (stock == null) {
                log.warn("Update stock rejected: null stock for product id={}", id);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Stock tidak boleh kosong"));
            }
            ProdukResponse produk = produkService.updateStock(id, stock);
            log.info("Product stock updated: id={} stock={} status={}",
                    id, produk.getStock(), produk.getStatus());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Stock produk berhasil diupdate",
                    "data", produk));
        } catch (Exception e) {
            log.error("Failed to update stock for product id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}/varian")
    public ResponseEntity<Map<String, Object>> getVarianByProduk(@PathVariable Long id) {
        try {
            log.info("GET /api/produk/{}/varian", id);
            List<ProdukVarianResponse> varianList = varianService.getByProduk(id, true);
            log.info("Found {} variants for productId={}", varianList.size(), id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data varian berhasil diambil",
                    "data", varianList));
        } catch (Exception e) {
            log.error("Failed to fetch variants for productId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Gagal mengambil data varian"));
        }
    }

    @PostMapping("/{id}/varian")
    public ResponseEntity<Map<String, Object>> createVarian(
            @PathVariable Long id,
            @Valid @RequestBody ProdukVarianRequest request,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("POST /api/produk/{}/varian nama={} stock={}", id, request.getNama(), request.getStock());
            ProdukVarianResponse varian = varianService.create(id, request);
            log.info("Variant created: variantId={} productId={} nama={}",
                    varian.getId(), id, varian.getNama());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Varian berhasil dibuat",
                            "data", varian));
        } catch (Exception e) {
            log.error("Failed to create variant for productId={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/varian/{id}")
    public ResponseEntity<Map<String, Object>> updateVarian(
            @PathVariable Long id,
            @Valid @RequestBody ProdukVarianRequest request,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("PUT /api/produk/varian/{} nama={} stock={}", id, request.getNama(), request.getStock());
            ProdukVarianResponse varian = varianService.update(id, request);
            log.info("Variant updated: variantId={} nama={}", varian.getId(), varian.getNama());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Varian berhasil diupdate",
                    "data", varian));
        } catch (Exception e) {
            log.error("Failed to update variant id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/varian/{id}")
    public ResponseEntity<Map<String, Object>> deleteVarian(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            requireAdmin(authentication);

            log.info("DELETE /api/produk/varian/{}", id);
            varianService.delete(id);
            log.info("Variant deactivated: variantId={}", id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Varian berhasil dinonaktifkan"));
        } catch (Exception e) {
            log.error("Failed to delete variant id={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    private static boolean gambarRequestTidakValid(ProdukRequest request) {
        if (urlGambarTidakValid(request.getGambar())) {
            return true;
        }
        return request.getGambarList() != null
                && request.getGambarList().stream().anyMatch(ProdukController::urlGambarTidakValid);
    }

    private static boolean urlGambarTidakValid(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        return !trimmed.startsWith("http://") && !trimmed.startsWith("https://");
    }
}
