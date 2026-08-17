package com.projekfajar.restock.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.restock.dto.RestockNotifikasiRequest;
import com.projekfajar.restock.dto.RestockNotifikasiResponse;
import com.projekfajar.restock.service.RestockNotifikasiService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/restock")
@RequiredArgsConstructor
@Slf4j
public class RestockNotifikasiController {

    private final RestockNotifikasiService restockService;
    private final SecurityUtils securityUtils;

    @PostMapping("/notifikasi")
    public ResponseEntity<Map<String, Object>> daftar(
            @RequestBody RestockNotifikasiRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("POST /api/restock/notifikasi rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("POST /api/restock/notifikasi userId={} productId={} variantId={}",
                    user.getId(), request.getProdukId(), request.getVariantId());
            RestockNotifikasiResponse response = restockService.daftar(user.getId(), request);
            log.info("Restock subscription registered: id={} userId={} productId={}",
                    response.getId(), user.getId(), request.getProdukId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Notifikasi restock berhasil didaftarkan",
                    "data", response));
        } catch (Exception e) {
            log.error("Failed to register restock notification for productId={}: {}",
                    request.getProdukId(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/notifikasi")
    public ResponseEntity<Map<String, Object>> getByUser(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/restock/notifikasi rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/restock/notifikasi for userId={}", user.getId());
            List<RestockNotifikasiResponse> list = restockService.getByUser(user.getId());
            log.info("Returned {} restock subscriptions for userId={}", list.size(), user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar notifikasi restock berhasil diambil",
                    "data", list));
        } catch (Exception e) {
            log.error("Failed to fetch restock notifications: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/notifikasi/{id}")
    public ResponseEntity<Map<String, Object>> batal(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("DELETE /api/restock/notifikasi/{} rejected: unauthenticated request", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("DELETE /api/restock/notifikasi/{} by userId={}", id, user.getId());
            restockService.batal(user.getId(), id);
            log.info("Restock subscription cancelled: id={} userId={}", id, user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notifikasi restock dibatalkan"));
        } catch (Exception e) {
            log.error("Failed to cancel restock notification id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
