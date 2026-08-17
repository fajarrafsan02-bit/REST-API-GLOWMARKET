package com.projekfajar.chat.controller;

import com.projekfajar.chat.dto.ChatConversationResponse;
import com.projekfajar.chat.dto.ChatMessageRequest;
import com.projekfajar.chat.dto.ChatMessageResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.chat.service.ChatService;
import com.projekfajar.chat.service.OnlineUserTracker;
import com.projekfajar.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final OnlineUserTracker onlineUserTracker;
    private final SecurityUtils securityUtils;
    
    /**
     * WebSocket endpoint for sending messages
     * User sends to: /app/chat.send
     * Admin sends to: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        try {
            User sender = securityUtils.getCurrentUser((Authentication) principal);
            
            log.info("WebSocket message from {} to {}", sender.getId(), request.getReceiverId());
            chatService.sendMessage(sender.getId(), request);
            
        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage(), e);
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
            User sender = securityUtils.getCurrentUser(authentication);
            if (sender == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            ChatMessageResponse response = chatService.sendMessage(sender.getId(), request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pesan berhasil dikirim",
                    "data", response));
                    
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
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
            log.info("[DEBUG] getChatHistory called with userId={}, adminId={}", userId, adminId);
            
            User currentUser = securityUtils.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            log.info("[DEBUG] Authenticated user email: {}", currentUser.getEmail());
            
            log.info("[DEBUG] Current user: id={}, role={}", currentUser.getId(), currentUser.getRole());
            
            Long finalUserId;
            Long finalAdminId;
            
            if (securityUtils.isAdmin(currentUser)) {
                // Admin requesting history with a user
                if (userId == null) {
                    log.warn("[DEBUG] Admin calling without userId parameter");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "User ID diperlukan"));
                }
                finalUserId = userId;
                finalAdminId = currentUser.getId();
                log.info("[DEBUG] Admin mode: finalUserId={}, finalAdminId={}", finalUserId, finalAdminId);
            } else {
                // User requesting history with admin
                if (adminId == null) {
                    log.warn("[DEBUG] User calling without adminId parameter");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "Admin ID diperlukan"));
                }
                finalUserId = currentUser.getId();
                finalAdminId = adminId;
                log.info("[DEBUG] User mode: finalUserId={}, finalAdminId={}", finalUserId, finalAdminId);
            }
            
            List<ChatMessageResponse> messages = chatService.getChatHistory(finalUserId, finalAdminId);
            log.info("[DEBUG] Retrieved {} messages", messages.size());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Riwayat chat berhasil diambil",
                    "data", messages));
                    
        } catch (Exception e) {
            log.error("Error getting chat history: {}", e.getMessage(), e);
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
            User admin = securityUtils.getCurrentUser(authentication);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            if (!securityUtils.isAdmin(admin)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }
            
            List<ChatConversationResponse> conversations = chatService.getAdminConversations(admin.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar percakapan berhasil diambil",
                    "data", conversations));
                    
        } catch (Exception e) {
            log.error("Error getting conversations: {}", e.getMessage(), e);
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
            User receiver = securityUtils.getCurrentUser(authentication);
            if (receiver == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            chatService.markMessagesAsRead(receiver.getId(), senderId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pesan berhasil ditandai sebagai dibaca"));
                    
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
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
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            Long unreadCount = chatService.getUnreadCount(user.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Jumlah pesan belum dibaca berhasil diambil",
                    "data", Map.of("unreadCount", unreadCount)));
                    
        } catch (Exception e) {
            log.error("Error getting unread count: {}", e.getMessage(), e);
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
            User admin = securityUtils.getCurrentUser(authentication);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            if (!securityUtils.isAdmin(admin)) {
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
            log.error("Error getting user status: {}", e.getMessage(), e);
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
            User admin = securityUtils.getCurrentUser(authentication);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            if (!securityUtils.isAdmin(admin)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }
            
            Set<Long> onlineIds = onlineUserTracker.getOnlineUserIds();
            List<User> users = userRepository.findAllById(onlineIds);
            
            List<Map<String, Object>> data = users.stream()
                    .filter(u -> u.getRole() == com.projekfajar.auth.model.Role.USER)
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
            log.error("Error getting online users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}