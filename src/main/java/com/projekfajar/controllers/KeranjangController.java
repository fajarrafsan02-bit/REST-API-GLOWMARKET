package com.projekfajar.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.DTO.KeranjangRequest;
import com.projekfajar.DTO.KeranjangResponse;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.KeranjangService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/keranjang")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class KeranjangController {
        private static final Logger logger = LoggerFactory.getLogger(KeranjangController.class);

        private final KeranjangService keranjangService;
        private final UserRepository userRepository;

        @GetMapping
        public ResponseEntity<Map<String, Object>> getKeranjang(Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        List<KeranjangResponse> keranjang = keranjangService.getKeranjangByUser(user.getId());

                        double total = keranjang.stream()
                                        .mapToDouble(KeranjangResponse::getSubtotal)
                                        .sum();

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Data keranjang berhasil diambil",
                                        "data", keranjang,
                                        "total", total));
                } catch (Exception e) {
                        logger.error("Error getting cart: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", "Gagal mengambil data keranjang"));
                }
        }

        @PostMapping
        public ResponseEntity<Map<String, Object>> addToKeranjang(
                        @Valid @RequestBody KeranjangRequest request,
                        Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        KeranjangResponse keranjang = keranjangService.addToKeranjang(user.getId(), request);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(Map.of(
                                                        "success", true,
                                                        "message", "Produk berhasil ditambahkan ke keranjang",
                                                        "data", keranjang));
                } catch (Exception e) {
                        logger.error("Error adding to cart: {}", e.getMessage(), e);
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
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

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
                        logger.error("Error updating quantity: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Map<String, Object>> removeFromKeranjang(
                        @PathVariable Long id,
                        Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        keranjangService.removeFromKeranjang(user.getId(), id);
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Produk berhasil dihapus dari keranjang"));
                } catch (Exception e) {
                        logger.error("Error removing from cart: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        @DeleteMapping("/clear")
        public ResponseEntity<Map<String, Object>> clearKeranjang(Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        keranjangService.clearKeranjang(user.getId());
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Keranjang berhasil dikosongkan"));
                } catch (Exception e) {
                        logger.error("Error clearing cart: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", "Gagal mengosongkan keranjang"));
                }
        }
}