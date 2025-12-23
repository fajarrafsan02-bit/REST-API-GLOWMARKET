package com.projekfajar.controllers;

import com.projekfajar.DTO.PesananResponse;
import com.projekfajar.DTO.UpdateStatusRequest;
import com.projekfajar.models.OrderStatus;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.PesananService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pesanan")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PesananController {
    private static final Logger logger = LoggerFactory.getLogger(PesananController.class);

    private final PesananService pesananService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPesanan(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Admin can see all orders
            List<PesananResponse> pesananList;
            if (user.getRole() == com.projekfajar.models.Role.ADMIN) {
                pesananList = pesananService.getAllPesanan();
            } else {
                pesananList = pesananService.getPesananByUser(user.getId());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar pesanan berhasil diambil",
                    "data", pesananList));
        } catch (Exception e) {
            logger.error("Error getting pesanan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPesananById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            PesananResponse pesanan = pesananService.getAllPesanan().stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));

            // Check if user owns the order or is admin
            if (!pesanan.getUserId().equals(user.getId()) && user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Anda tidak memiliki akses ke pesanan ini"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Detail pesanan berhasil diambil",
                    "data", pesanan));
        } catch (Exception e) {
            logger.error("Error getting pesanan detail: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/nomor/{nomorPesanan}")
    public ResponseEntity<Map<String, Object>> getPesananByNomor(
            @PathVariable String nomorPesanan,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            PesananResponse pesanan = pesananService.getPesananByNomor(nomorPesanan);

            // Check if user owns the order or is admin
            if (!pesanan.getUserId().equals(user.getId()) && user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Anda tidak memiliki akses ke pesanan ini"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Detail pesanan berhasil diambil",
                    "data", pesanan));
        } catch (Exception e) {
            logger.error("Error getting pesanan by nomor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can update status
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah status pesanan"));
            }

            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus());
            PesananResponse updated = pesananService.updateStatus(id, newStatus, request.getNomorResi());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status pesanan berhasil diperbarui",
                    "data", updated));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Status tidak valid"));
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/user/history")
    public ResponseEntity<Map<String, Object>> getUserPesanan(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            List<PesananResponse> pesananList = pesananService.getPesananByUser(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Riwayat pesanan berhasil diambil",
                    "data", pesananList));
        } catch (Exception e) {
            logger.error("Error getting user pesanan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
