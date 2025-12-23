package com.projekfajar.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.DTO.NotificationMessage;
import com.projekfajar.models.Notification;
import com.projekfajar.models.Produk;
import com.projekfajar.models.Role;
import com.projekfajar.models.User;
import com.projekfajar.repository.NotificationRepository;
import com.projekfajar.repository.ProdukRepository;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminNotificationController {

        private static final Logger logger = LoggerFactory.getLogger(AdminNotificationController.class);
        
        // Admin notification types
        private static final List<String> ADMIN_NOTIFICATION_TYPES = Arrays.asList(
                "NEW_CUSTOMER",
                "NEW_ORDER",
                "LOW_STOCK"
        );

        private final NotificationRepository notificationRepository;
        private final UserRepository userRepository;
        private final NotificationService notificationService;
        private final ProdukRepository produkRepository;

        @GetMapping
        public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        if (user.getRole() != Role.ADMIN) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("success", false, "message",
                                                                "Hanya admin yang dapat mengakses data ini"));
                        }
                        
                        // Get admin notifications by TYPE (NEW_CUSTOMER, NEW_ORDER, LOW_STOCK)
                        List<Notification> entities = notificationRepository.findTop50ByTypeInOrderByCreatedAtDesc(ADMIN_NOTIFICATION_TYPES);
                        Long unreadCount = notificationRepository.countByTypeInAndReadFalse(ADMIN_NOTIFICATION_TYPES);

                        List<NotificationMessage> notifications = entities.stream()
                                        .map(n -> NotificationMessage.builder()
                                                        .id(n.getId())
                                                        .type(n.getType())
                                                        .title(n.getTitle())
                                                        .message(n.getMessage())
                                                        .userId(n.getUserId())
                                                        .paymentId(n.getPaymentId())
                                                        .produkId(n.getProdukId())
                                                        .timestamp(n.getCreatedAt())
                                                        .isRead(n.getRead())
                                                        .readAt(n.getReadAt())
                                                        .build())
                                        .collect(Collectors.toList());

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Data notifikasi berhasil diambil",
                                        "data", notifications,
                                        "unreadCount", unreadCount));
                } catch (Exception e) {
                        logger.error("Error getting notifications: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("success", false, "message", "Gagal memuat data notifikasi"));
                }
        }

        @PutMapping("/mark-all-read")
        public ResponseEntity<Map<String, Object>> markAllRead(Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        if (user.getRole() != Role.ADMIN) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("success", false, "message",
                                                                "Hanya admin yang dapat mengakses data ini"));
                        }

                        LocalDateTime now = LocalDateTime.now();

                        // Update admin notifications by TYPE
                        List<Notification> unreadNotifications = notificationRepository.findByTypeInAndReadFalse(ADMIN_NOTIFICATION_TYPES);

                        for (Notification n : unreadNotifications) {
                                n.setRead(true);
                                n.setReadAt(now);
                                notificationRepository.save(n);
                        }

                        logger.info("Marked {} notifications as read", unreadNotifications.size());

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Semua notifikasi ditandai sudah dibaca",
                                        "updated", unreadNotifications.size()));
                } catch (Exception e) {
                        logger.error("Error marking notifications as read: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("success", false, "message", "Gagal memperbarui notifikasi"));
                }
        }

        @PutMapping("/{id}/read")
        public ResponseEntity<Map<String, Object>> markAsRead(
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

                        if (user.getRole() != Role.ADMIN) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("success", false, "message",
                                                                "Hanya admin yang dapat mengakses data ini"));
                        }

                        // Find notification by ID
                        Notification notification = notificationRepository.findById(id)
                                        .orElseThrow(() -> new RuntimeException("Notifikasi tidak ditemukan"));

                        // If already read, return success without updating
                        if (Boolean.TRUE.equals(notification.getRead())) {
                                return ResponseEntity.ok(Map.of(
                                                "success", true,
                                                "message", "Notifikasi sudah ditandai sebagai dibaca",
                                                "alreadyRead", true));
                        }

                        // Mark as read
                        LocalDateTime now = LocalDateTime.now();
                        notification.setRead(true);
                        notification.setReadAt(now);
                        notificationRepository.save(notification);

                        logger.info("Notification {} marked as read", id);

                        // Return updated notification data
                        NotificationMessage responseData = NotificationMessage.builder()
                                        .id(notification.getId())
                                        .type(notification.getType())
                                        .title(notification.getTitle())
                                        .message(notification.getMessage())
                                        .userId(notification.getUserId())
                                        .paymentId(notification.getPaymentId())
                                        .produkId(notification.getProdukId())
                                        .timestamp(notification.getCreatedAt())
                                        .isRead(notification.getRead())
                                        .readAt(notification.getReadAt())
                                        .build();

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Notifikasi ditandai sudah dibaca",
                                        "data", responseData));
                } catch (Exception e) {
                        logger.error("Error marking notification as read: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("success", false, "message",
                                                        "Gagal memperbarui notifikasi: " + e.getMessage()));
                }
        }

        @GetMapping("/check-low-stock")
        public ResponseEntity<Map<String, Object>> checkLowStock(Authentication authentication) {
                try {
                        if (authentication == null) {
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                                .body(Map.of("success", false, "message",
                                                                "Silakan login terlebih dahulu"));
                        }

                        String email = authentication.getName();
                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

                        if (user.getRole() != Role.ADMIN) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("success", false, "message",
                                                                "Hanya admin yang dapat mengakses data ini"));
                        }

                        // Get all products
                        List<Produk> allProducts = produkRepository.findAll();
                        
                        // Check and send notifications for low stock products
                        notificationService.checkAndNotifyLowStockProducts(allProducts);
                        
                        // Count low stock products
                        long lowStockCount = allProducts.stream()
                                .filter(p -> p.getStock() != null && p.getStock() < 5)
                                .count();

                        logger.info("Low stock check completed, found {} products", lowStockCount);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Pengecekan stok berhasil",
                                        "lowStockCount", lowStockCount));
                } catch (Exception e) {
                        logger.error("Error checking low stock: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("success", false, "message",
                                                        "Gagal memeriksa stok: " + e.getMessage()));
                }
        }
}
