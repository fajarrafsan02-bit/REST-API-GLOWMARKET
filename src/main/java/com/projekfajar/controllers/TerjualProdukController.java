package com.projekfajar.controllers;

import com.projekfajar.DTO.TerjualProdukResponse;
import com.projekfajar.models.TerjualProduk;
import com.projekfajar.models.User;
import com.projekfajar.repository.TerjualProdukRepository;
import com.projekfajar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/terjual-produk")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TerjualProdukController {
        private static final Logger logger = LoggerFactory.getLogger(TerjualProdukController.class);

        private final TerjualProdukRepository terjualProdukRepository;
        private final UserRepository userRepository;

        @GetMapping
        public ResponseEntity<Map<String, Object>> getAllTerjualProduk(Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        // Only admin can access
                        if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("success", false, "message",
                                                                "Hanya admin yang dapat mengakses data ini"));
                        }

                        List<TerjualProdukResponse> responses = terjualProdukRepository.findAll().stream()
                                        .map(this::convertToResponse)
                                        .collect(Collectors.toList());

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Data produk terjual berhasil diambil",
                                        "data", responses));
                } catch (Exception e) {
                        logger.error("Error getting terjual produk: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        @GetMapping("/{produkId}")
        public ResponseEntity<Map<String, Object>> getTerjualByProdukId(
                        @PathVariable Long produkId,
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

                        // Only admin can access
                        if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("success", false, "message",
                                                                "Hanya admin yang dapat mengakses data ini"));
                        }

                        TerjualProduk terjualProduk = terjualProdukRepository.findByProdukId(produkId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Data tidak ditemukan untuk produk ID: " + produkId));

                        TerjualProdukResponse response = convertToResponse(terjualProduk);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Data produk terjual berhasil diambil",
                                        "data", response));
                } catch (Exception e) {
                        logger.error("Error getting terjual produk by ID: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("success", false, "message", e.getMessage()));
                }
        }

        private TerjualProdukResponse convertToResponse(TerjualProduk terjualProduk) {
                return TerjualProdukResponse.builder()
                                .id(terjualProduk.getId())
                                .produkId(terjualProduk.getProduk().getId())
                                .namaProduk(terjualProduk.getProduk().getNama())
                                .harga(terjualProduk.getProduk().getHarga())
                                .terjual(terjualProduk.getTerjual())
                                .karatEmas(terjualProduk.getProduk().getKaratEmas())
                                .build();
        }
}
