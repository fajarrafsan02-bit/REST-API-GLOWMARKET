package com.projekfajar.controllers;

import com.projekfajar.DTO.ChatConversationResponse;
import com.projekfajar.DTO.ChatMessageRequest;
import com.projekfajar.DTO.ChatMessageResponse;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.ChatService;
import com.projekfajar.services.OnlineUserTracker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final OnlineUserTracker onlineUserTracker;
    
    /**
     * WebSocket endpoint for sending messages
     * User sends to: /app/chat.send
     * Admin sends to: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        try {
            String email = principal.getName();
            User sender = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            logger.info("WebSocket message from {} to {}", sender.getId(), request.getReceiverId());
            chatService.sendMessage(sender.getId(), request);
            
        } catch (Exception e) {
            logger.error("Error sending WebSocket message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * REST endpoint for sending messages (alternative to WebSocket)
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessageRest(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User sender = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            ChatMessageResponse response = chatService.sendMessage(sender.getId(), request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pesan berhasil dikirim",
                    "data", response));
                    
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get chat history between user and admin
     * For user: GET /api/chat/history?adminId=1
     * For admin: GET /api/chat/history?userId=2
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long adminId,
            Authentication authentication) {
        try {
            logger.info("[DEBUG] getChatHistory called with userId={}, adminId={}", userId, adminId);
            
            if (authentication == null) {
                logger.warn("[DEBUG] No authentication provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            logger.info("[DEBUG] Authenticated user email: {}", email);
            
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            logger.info("[DEBUG] Current user: id={}, role={}", currentUser.getId(), currentUser.getRole());
            
            Long finalUserId;
            Long finalAdminId;
            
            if (currentUser.getRole() == com.projekfajar.models.Role.ADMIN) {
                // Admin requesting history with a user
                if (userId == null) {
                    logger.warn("[DEBUG] Admin calling without userId parameter");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "User ID diperlukan"));
                }
                finalUserId = userId;
                finalAdminId = currentUser.getId();
                logger.info("[DEBUG] Admin mode: finalUserId={}, finalAdminId={}", finalUserId, finalAdminId);
            } else {
                // User requesting history with admin
                if (adminId == null) {
                    logger.warn("[DEBUG] User calling without adminId parameter");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Admin ID diperlukan"));
                }
                finalUserId = currentUser.getId();
                finalAdminId = adminId;
                logger.info("[DEBUG] User mode: finalUserId={}, finalAdminId={}", finalUserId, finalAdminId);
            }
            
            List<ChatMessageResponse> messages = chatService.getChatHistory(finalUserId, finalAdminId);
            logger.info("[DEBUG] Retrieved {} messages", messages.size());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Riwayat chat berhasil diambil",
                    "data", messages));
                    
        } catch (Exception e) {
            logger.error("Error getting chat history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get all conversations for admin (list of users)
     * Only for ADMIN role
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getAdminConversations(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            if (admin.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }
            
            List<ChatConversationResponse> conversations = chatService.getAdminConversations(admin.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar percakapan berhasil diambil",
                    "data", conversations));
                    
        } catch (Exception e) {
            logger.error("Error getting conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Mark messages as read
     */
    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @RequestParam Long senderId,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User receiver = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            chatService.markMessagesAsRead(receiver.getId(), senderId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pesan berhasil ditandai sebagai dibaca"));
                    
        } catch (Exception e) {
            logger.error("Error marking messages as read: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get unread message count
     */
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
            
            Long unreadCount = chatService.getUnreadCount(user.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Jumlah pesan belum dibaca berhasil diambil",
                    "data", Map.of("unreadCount", unreadCount)));
                    
        } catch (Exception e) {
            logger.error("Error getting unread count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get online status for a specific user (admin only)
     */
    @GetMapping("/user-status/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStatus(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            if (admin.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }
            
            boolean online = onlineUserTracker.isUserOnline(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status user berhasil diambil",
                    "data", Map.of(
                            "userId", userId,
                            "online", online)));
                    
        } catch (Exception e) {
            logger.error("Error getting user status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    /**
     * Get list of online users (admin only)
     */
    @GetMapping("/online-users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            if (admin.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }
            
            Set<Long> onlineIds = onlineUserTracker.getOnlineUserIds();
            List<User> users = userRepository.findAllById(onlineIds);
            
            List<Map<String, Object>> data = users.stream()
                    .filter(u -> u.getRole() == com.projekfajar.models.Role.USER)
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("namaLengkap", u.getNamaLengkap());
                        map.put("email", u.getEmail());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar user online berhasil diambil",
                    "data", data));
                    
        } catch (Exception e) {
            logger.error("Error getting online users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}