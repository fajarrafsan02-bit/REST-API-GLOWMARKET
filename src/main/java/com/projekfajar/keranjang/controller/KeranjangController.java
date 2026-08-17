package com.projekfajar.keranjang.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.keranjang.dto.KeranjangRequest;
import com.projekfajar.keranjang.dto.KeranjangResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.keranjang.service.KeranjangService;
import com.projekfajar.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/keranjang")
@RequiredArgsConstructor
@Slf4j
public class KeranjangController {

        private final KeranjangService keranjangService;
        private final SecurityUtils securityUtils;

        @GetMapping
        public ResponseEntity<Map<String, Object>> getKeranjang(Authentication authentication) {
                try {
                        User user = securityUtils.getCurrentUser(authentication);
                        if (user == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
                        }

                        List<KeranjangResponse> keranjang = keranjangService.getKeranjangByUser(user.getId());

                        BigDecimal total = keranjang.stream()
                                        .map(KeranjangResponse::getSubtotal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Data keranjang berhasil diambil",
                                        "data", keranjang,
                                        "total", total));
                } catch (Exception e) {
                        log.error("Error getting cart: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", "Gagal mengambil data keranjang"));
                }
        }

        @PostMapping
        public ResponseEntity<Map<String, Object>> addToKeranjang(
                        @Valid @RequestBody KeranjangRequest request,
                        Authentication authentication) {
                try {
                        User user = securityUtils.getCurrentUser(authentication);
                        if (user == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
                        }

                        KeranjangResponse keranjang = keranjangService.addToKeranjang(user.getId(), request);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(Map.of(
                                                        "success", true,
                                                        "message", "Produk berhasil ditambahkan ke keranjang",
                                                        "data", keranjang));
                } catch (Exception e) {
                        log.error("Error adding to cart: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        @PatchMapping("/{id}")
        public ResponseEntity<Map<String, Object>> updateQuantity(
                        @PathVariable Long id,
                        @RequestBody Map<String, Integer> request,
                        Authentication authentication) {
                try {
                        User user = securityUtils.getCurrentUser(authentication);
                        if (user == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
                        }

                        Integer quantity = request.get("quantity");
                        if (quantity == null) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("success", false, "message",
                                                                "Quantity tidak boleh kosong"));
                        }

                        KeranjangResponse keranjang = keranjangService.updateQuantity(user.getId(), id, quantity);
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Quantity berhasil diupdate",
                                        "data", keranjang));
                } catch (Exception e) {
                        log.error("Error updating quantity: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Map<String, Object>> removeFromKeranjang(
                        @PathVariable Long id,
                        Authentication authentication) {
                try {
                        User user = securityUtils.getCurrentUser(authentication);
                        if (user == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
                        }

                        keranjangService.removeFromKeranjang(user.getId(), id);
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Produk berhasil dihapus dari keranjang"));
                } catch (Exception e) {
                        log.error("Error removing from cart: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        @DeleteMapping("/clear")
        public ResponseEntity<Map<String, Object>> clearKeranjang(Authentication authentication) {
                try {
                        User user = securityUtils.getCurrentUser(authentication);
                        if (user == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
                        }

                        keranjangService.clearKeranjang(user.getId());
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Keranjang berhasil dikosongkan"));
                } catch (Exception e) {
                        log.error("Error clearing cart: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", "Gagal mengosongkan keranjang"));
                }
        }
}