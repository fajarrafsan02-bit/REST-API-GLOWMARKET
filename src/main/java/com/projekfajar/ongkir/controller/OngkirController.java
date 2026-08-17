package com.projekfajar.ongkir.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.alamat.repository.AlamatRepository;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.keranjang.model.Keranjang;
import com.projekfajar.keranjang.repository.KeranjangRepository;
import com.projekfajar.ongkir.dto.EstimasiOngkirRequest;
import com.projekfajar.ongkir.dto.HasilOngkir;
import com.projekfajar.ongkir.model.Ongkir;
import com.projekfajar.ongkir.service.OngkirCalculationService;
import com.projekfajar.ongkir.service.OngkirService;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ongkir")
@RequiredArgsConstructor
@Slf4j
public class OngkirController {

    private final OngkirService ongkirService;
    private final OngkirCalculationService ongkirCalculationService;
    private final AlamatRepository alamatRepository;
    private final KeranjangRepository keranjangRepository;
    private final SecurityUtils securityUtils;

    /**
     * Dipakai halaman Keranjang untuk menampilkan perkiraan ongkir sebelum
     * pembeli masuk ke Checkout — menghitung dari isi keranjang saat ini,
     * lewat titik perhitungan yang sama dipakai checkout sungguhan
     * (OngkirCalculationService), supaya angkanya tidak pernah berbeda.
     */
    @PostMapping("/estimasi")
    public ResponseEntity<Map<String, Object>> estimasi(
            @Valid @RequestBody EstimasiOngkirRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            Alamat alamat = alamatRepository.findById(request.getAlamatId())
                    .orElseThrow(() -> new BusinessException("Alamat tidak ditemukan"));

            if (alamat.getUser() != null && !alamat.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Alamat tidak valid");
            }

            List<Keranjang> cartItems = keranjangRepository.findByUserId(user.getId());
            if (cartItems.isEmpty()) {
                throw new BusinessException("Keranjang kosong");
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            int totalBeratGram = 0;
            for (Keranjang item : cartItems) {
                Produk produk = item.getProduk();
                subtotal = subtotal.add(produk.getHarga().multiply(BigDecimal.valueOf(item.getQuantity())));
                if (produk.getBeratGram() != null) {
                    totalBeratGram += (int) Math.ceil(produk.getBeratGram() * item.getQuantity());
                }
            }

            // hitung() tanpa pilihan = opsi termurah (perilaku lama) + field
            // opsi (daftar kurir+layanan) diisi kalau RajaOngkir dipakai.
            HasilOngkir hasil = ongkirCalculationService.hitung(alamat, totalBeratGram, subtotal);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estimasi ongkir berhasil dihitung",
                    "data", hasil));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error menghitung estimasi ongkir: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal menghitung estimasi ongkir"));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        try {
            List<Ongkir> list = ongkirService.getAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar tarif ongkir berhasil diambil",
                    "data", list));
        } catch (Exception e) {
            log.error("Error getting ongkir list: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat tarif ongkir"));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah tarif ongkir"));
            }

            Ongkir created = ongkirService.create(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tarif ongkir berhasil ditambahkan",
                    "data", created));
        } catch (Exception e) {
            log.error("Error creating ongkir: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah tarif ongkir"));
            }

            Ongkir updated = ongkirService.update(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tarif ongkir berhasil diperbarui",
                    "data", updated));
        } catch (Exception e) {
            log.error("Error updating ongkir: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah tarif ongkir"));
            }

            ongkirService.delete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tarif ongkir berhasil dihapus"));
        } catch (Exception e) {
            log.error("Error deleting ongkir: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}