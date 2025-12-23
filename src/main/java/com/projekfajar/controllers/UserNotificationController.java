package com.projekfajar.controllers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.projekfajar.models.User;
import com.projekfajar.repository.NotificationRepository;
import com.projekfajar.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(UserNotificationController.class);
    
    // User notification types
    private static final List<String> USER_NOTIFICATION_TYPES = Arrays.asList(
            "ORDER_SHIPPED",
            "ORDER_COMPLETED"
    );

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Get user notifications by TYPE (ORDER_SHIPPED, ORDER_COMPLETED) and userId
            List<Notification> entities = notificationRepository
                    .findByTypeInAndUserIdOrderByCreatedAtDesc(USER_NOTIFICATION_TYPES, user.getId());
            Long unreadCount = notificationRepository
                    .countByTypeInAndUserIdAndReadFalse(USER_NOTIFICATION_TYPES, user.getId());

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
            logger.error("Error getting user notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat data notifikasi"));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            Long unreadCount = notificationRepository
                    .countByTypeInAndUserIdAndReadFalse(USER_NOTIFICATION_TYPES, user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Jumlah notifikasi belum dibaca berhasil diambil",
                    "unreadCount", unreadCount));
        } catch (Exception e) {
            logger.error("Error getting unread count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat data"));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
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

            // Find notification by ID
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notifikasi tidak ditemukan"));

            // Verify ownership: check userId matches
            if (notification.getUserId() == null || !notification.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Tidak memiliki akses ke notifikasi ini"));
            }

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

            logger.info("User notification {} marked as read by user {}", id, user.getId());

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

    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            LocalDateTime now = LocalDateTime.now();

            // Update user notifications by TYPE and userId
            List<Notification> unreadNotifications = notificationRepository
                    .findByTypeInAndUserIdAndReadFalse(USER_NOTIFICATION_TYPES, user.getId());

            for (Notification n : unreadNotifications) {
                n.setRead(true);
                n.setReadAt(now);
                notificationRepository.save(n);
            }

            logger.info("Marked {} notifications as read for user {}", unreadNotifications.size(), user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Semua notifikasi ditandai sudah dibaca",
                    "updated", unreadNotifications.size()));
        } catch (Exception e) {
            logger.error("Error marking all notifications as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memperbarui notifikasi"));
        }
    }
}
