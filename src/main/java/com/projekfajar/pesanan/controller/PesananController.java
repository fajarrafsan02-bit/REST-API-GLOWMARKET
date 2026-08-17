package com.projekfajar.pesanan.controller;

import com.projekfajar.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.pesanan.dto.PesananResponse;
import com.projekfajar.pesanan.dto.UpdateStatusRequest;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.user.model.User;
import com.projekfajar.pesanan.service.PesananService;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/pesanan")
@RequiredArgsConstructor
@Slf4j
public class PesananController {

    private final PesananService pesananService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPesanan(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/pesanan rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/pesanan by userId={} admin={}", user.getId(), securityUtils.isAdmin(user));
            List<PesananResponse> pesananList = securityUtils.isAdmin(user)
                    ? pesananService.getAllPesanan()
                    : pesananService.getPesananByUser(user.getId());

            log.info("GET /api/pesanan returned {} orders for userId={}", pesananList.size(), user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar pesanan berhasil diambil",
                    "data", pesananList));
        } catch (Exception e) {
            log.error("Failed to fetch orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPesananById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/pesanan/{} rejected: unauthenticated request", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/pesanan/{} by userId={}", id, user.getId());
            PesananResponse pesanan = pesananService.getAllPesanan().stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));

            if (!pesanan.getUserId().equals(user.getId()) && !securityUtils.isAdmin(user)) {
                log.warn("Access denied to orderId={} for userId={}", id, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Anda tidak memiliki akses ke pesanan ini"));
            }

            log.info("Order detail returned: orderId={} nomor={}", id, pesanan.getNomorPesanan());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Detail pesanan berhasil diambil",
                    "data", pesanan));
        } catch (Exception e) {
            log.error("Failed to fetch order detail orderId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/nomor/{nomorPesanan}")
    public ResponseEntity<Map<String, Object>> getPesananByNomor(
            @PathVariable String nomorPesanan,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/pesanan/nomor/{} rejected: unauthenticated request", nomorPesanan);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/pesanan/nomor/{} by userId={}", nomorPesanan, user.getId());
            PesananResponse pesanan = pesananService.getPesananByNomor(nomorPesanan);

            if (!pesanan.getUserId().equals(user.getId()) && !securityUtils.isAdmin(user)) {
                log.warn("Access denied to order nomor={} for userId={}", nomorPesanan, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Anda tidak memiliki akses ke pesanan ini"));
            }

            log.info("Order detail returned: nomor={} status={}", nomorPesanan, pesanan.getStatus());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Detail pesanan berhasil diambil",
                    "data", pesanan));
        } catch (Exception e) {
            log.error("Failed to fetch order by nomor={}: {}", nomorPesanan, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Mencari pesanan berdasarkan external ID invoice.
     * Dipakai halaman status pembayaran, yang hanya memegang external ID.
     */
    @GetMapping("/external/{externalId}")
    public ResponseEntity<Map<String, Object>> getPesananByExternalId(
            @PathVariable String externalId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/pesanan/external/{} rejected: unauthenticated request", externalId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/pesanan/external/{} by userId={}", externalId, user.getId());
            PesananResponse pesanan = pesananService.getPesananByExternalId(externalId);

            if (!pesanan.getUserId().equals(user.getId()) && !securityUtils.isAdmin(user)) {
                log.warn("Access denied to order externalId={} for userId={}", externalId, user.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Anda tidak memiliki akses ke pesanan ini"));
            }

            log.info("Order detail returned: externalId={} nomor={}", externalId, pesanan.getNomorPesanan());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Detail pesanan berhasil diambil",
                    "data", pesanan));
        } catch (Exception e) {
            log.error("Failed to fetch order by externalId={}: {}", externalId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("PUT /api/pesanan/{}/status rejected: unauthenticated request", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                log.warn("Non-admin userId={} attempted to update status of orderId={}", user.getId(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah status pesanan"));
            }

            log.info("PUT /api/pesanan/{}/status to={} by adminId={}", id, request.getStatus(), user.getId());
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus());
            PesananResponse updated = pesananService.updateStatus(id, newStatus, request.getNomorResi());

            log.info("Order status updated: orderId={} nomor={} status={}",
                    id, updated.getNomorPesanan(), updated.getStatus());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status pesanan berhasil diperbarui",
                    "data", updated));
        } catch (IllegalArgumentException e) {
            log.error("Invalid status '{}' for orderId={}: {}", request.getStatus(), id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Status tidak valid"));
        } catch (Exception e) {
            log.error("Failed to update status for orderId={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/user/history")
    public ResponseEntity<Map<String, Object>> getUserPesanan(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/pesanan/user/history rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/pesanan/user/history for userId={}", user.getId());
            List<PesananResponse> pesananList = pesananService.getPesananByUser(user.getId());

            log.info("Order history returned {} orders for userId={}", pesananList.size(), user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Riwayat pesanan berhasil diambil",
                    "data", pesananList));
        } catch (Exception e) {
            log.error("Failed to fetch order history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
