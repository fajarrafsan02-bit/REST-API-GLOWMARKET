package com.projekfajar.notification.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.user.model.User;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data notifikasi berhasil diambil",
                    "data", notificationService.getAdminNotifications(),
                    "unreadCount", notificationService.getAdminUnreadCount()));
        } catch (Exception e) {
            log.error("Error getting notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat data notifikasi"));
        }
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            int updated = notificationService.markAllAdminRead();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Semua notifikasi ditandai sudah dibaca",
                    "updated", updated));
        } catch (Exception e) {
            log.error("Error marking notifications as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memperbarui notifikasi"));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
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
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            Map<String, Object> result = notificationService.markAdminNotificationRead(id);
            boolean alreadyRead = (boolean) result.get("alreadyRead");

            if (alreadyRead) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Notifikasi sudah ditandai sebagai dibaca",
                        "alreadyRead", true));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notifikasi ditandai sudah dibaca",
                    "data", result.get("data")));
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            "Gagal memperbarui notifikasi: " + e.getMessage()));
        }
    }

    @GetMapping("/check-low-stock")
    public ResponseEntity<Map<String, Object>> checkLowStock(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            long lowStockCount = notificationService.checkLowStockProducts();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pengecekan stok berhasil",
                    "lowStockCount", lowStockCount));
        } catch (Exception e) {
            log.error("Error checking low stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            "Gagal memeriksa stok: " + e.getMessage()));
        }
    }
}
