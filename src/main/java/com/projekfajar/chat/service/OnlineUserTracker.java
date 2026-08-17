package com.projekfajar.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlineUserTracker {

    private static final Logger logger = LoggerFactory.getLogger(OnlineUserTracker.class);

    private final Map<Long, OnlineUserInfo> onlineUsers = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public OnlineUserTracker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void userConnected(Long userId, String sessionId, String role) {
        if (userId == null || sessionId == null) {
            return;
        }

        onlineUsers.compute(userId, (id, info) -> {
            if (info == null) {
                info = new OnlineUserInfo(id, role);
            }
            info.getSessionIds().add(sessionId);
            info.setLastSeen(Instant.now());
            return info;
        });

        sessionToUser.put(sessionId, userId);
        logger.info("User {} connected with session {} (role: {})", userId, sessionId, role);

        // Broadcast ONLINE status (hanya untuk USER, bukan ADMIN)
        if ("USER".equals(role)) {
            broadcastStatus(userId, "ONLINE");
        }
    }

    public void userDisconnected(String sessionId) {
        if (sessionId == null) {
            return;
        }

        Long userId = sessionToUser.remove(sessionId);
        if (userId == null) {
            return;
        }

        onlineUsers.computeIfPresent(userId, (id, info) -> {
            info.getSessionIds().remove(sessionId);
            info.setLastSeen(Instant.now());

            // Jika user tidak punya session lain (benar-benar offline)
            if (info.getSessionIds().isEmpty()) {
                logger.info("User {} went offline (session: {})", userId, sessionId);
                
                // Broadcast OFFLINE status (hanya untuk USER)
                if ("USER".equals(info.getRole())) {
                    broadcastStatus(userId, "OFFLINE");
                }
                
                return null; // remove entry
            }

            logger.info("User {} still has {} active session(s)", userId, info.getSessionIds().size());
            return info;
        });
    }

    public boolean isUserOnline(Long userId) {
        OnlineUserInfo info = onlineUsers.get(userId);
        return info != null && !info.getSessionIds().isEmpty();
    }

    public Set<Long> getOnlineUserIds() {
        return new HashSet<>(onlineUsers.keySet());
    }

    public Optional<Instant> getLastSeen(Long userId) {
        OnlineUserInfo info = onlineUsers.get(userId);
        return info != null ? Optional.ofNullable(info.getLastSeen()) : Optional.empty();
    }

    private void broadcastStatus(Long userId, String status) {
        try {
            Map<String, Object> presenceUpdate = new HashMap<>();
            presenceUpdate.put("userId", userId);
            presenceUpdate.put("status", status);
            presenceUpdate.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/user.presence", (Object) presenceUpdate);
            logger.info("Broadcasted {} status for user {}", status, userId);
        } catch (Exception e) {
            logger.warn("Failed to broadcast user status for {}: {}", userId, e.getMessage());
        }
    }

    private static class OnlineUserInfo {
        private final Long userId;
        private final String role;
        private final Set<String> sessionIds = ConcurrentHashMap.newKeySet();
        private Instant lastSeen;

        OnlineUserInfo(Long userId, String role) {
            this.userId = userId;
            this.role = role;
            this.lastSeen = Instant.now();
        }

        public Long getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }

        public Set<String> getSessionIds() {
            return sessionIds;
        }

        public Instant getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(Instant lastSeen) {
            this.lastSeen = lastSeen;
        }
    }
}
