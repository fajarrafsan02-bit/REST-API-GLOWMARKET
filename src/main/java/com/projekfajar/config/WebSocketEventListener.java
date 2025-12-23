package com.projekfajar.config;

import com.projekfajar.services.OnlineUserTracker;
import com.projekfajar.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final JwtUtil jwtUtil;
    private final OnlineUserTracker onlineUserTracker;

    public WebSocketEventListener(JwtUtil jwtUtil, OnlineUserTracker onlineUserTracker) {
        this.jwtUtil = jwtUtil;
        this.onlineUserTracker = onlineUserTracker;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String token = accessor.getFirstNativeHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("WebSocket CONNECT without valid Authorization header, sessionId={}", sessionId);
            return;
        }

        token = token.substring(7);

        try {
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractClaim(token, claims -> {
                Object r = claims.get("role");
                return r != null ? r.toString() : null;
            });

            if (userId == null) {
                logger.warn("Cannot extract userId from JWT on CONNECT, sessionId={}", sessionId);
                return;
            }

            onlineUserTracker.userConnected(userId, sessionId, role);
        } catch (Exception e) {
            logger.error("Failed to process WebSocket connect event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        try {
            onlineUserTracker.userDisconnected(sessionId);
        } catch (Exception e) {
            logger.error("Failed to process WebSocket disconnect event: {}", e.getMessage(), e);
        }
    }
}
