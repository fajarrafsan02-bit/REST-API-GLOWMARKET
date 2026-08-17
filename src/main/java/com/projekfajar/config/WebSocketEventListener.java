package com.projekfajar.config;

import com.projekfajar.chat.service.OnlineUserTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * userId/role sekarang datang dari session attributes yang diisi
 * WsHandshakeInterceptor saat handshake — bukan dengan mem-parse ulang token
 * dari header STOMP seperti sebelumnya (dua tempat mem-parse token yang sama
 * dengan cara berbeda adalah sumber bug yang gampang diam-diam berbeda hasil).
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final OnlineUserTracker onlineUserTracker;

    public WebSocketEventListener(OnlineUserTracker onlineUserTracker) {
        this.onlineUserTracker = onlineUserTracker;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (accessor.getSessionAttributes() == null) {
            logger.warn("WebSocket CONNECT tanpa session attributes, sessionId={}", sessionId);
            return;
        }

        Object userIdAttr = accessor.getSessionAttributes().get(WsHandshakeInterceptor.ATTR_USER_ID);
        Object roleAttr = accessor.getSessionAttributes().get(WsHandshakeInterceptor.ATTR_ROLE);

        if (!(userIdAttr instanceof Long userId)) {
            logger.warn("WebSocket CONNECT tanpa userId (handshake seharusnya sudah menolaknya), sessionId={}",
                    sessionId);
            return;
        }

        onlineUserTracker.userConnected(userId, sessionId, roleAttr != null ? roleAttr.toString() : null);
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
