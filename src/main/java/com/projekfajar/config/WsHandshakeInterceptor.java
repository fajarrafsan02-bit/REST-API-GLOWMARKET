package com.projekfajar.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.projekfajar.auth.service.AuthCookieService;
import com.projekfajar.util.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * Menggantikan cara lama: JS membaca token dari localStorage lalu memasangnya
 * sendiri sebagai header STOMP "Authorization" pada frame CONNECT. Cookie
 * httpOnly tidak bisa dibaca JS sama sekali, jadi autentikasi WebSocket
 * dipindah ke sini — saat handshake HTTP awal (upgrade SockJS/WebSocket),
 * satu-satunya titik di alur WS tempat browser masih mengirim cookie secara
 * otomatis seperti request HTTP biasa.
 *
 * Hasilnya dititipkan di session attributes (bukan SecurityContextHolder,
 * yang tidak bertahan melewati satu request) supaya WebSocketConfig dan
 * WebSocketEventListener tinggal membacanya tanpa mem-parse token lagi.
 */
@Component
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(WsHandshakeInterceptor.class);

    public static final String ATTR_AUTHENTICATION = "authentication";
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLE = "role";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final AuthCookieService authCookieService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = authCookieService.extractToken(servletRequest.getServletRequest());
        if (token == null) {
            logger.warn("WebSocket handshake ditolak: cookie autentikasi tidak ada");
            return false;
        }

        try {
            String email = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtUtil.isTokenValid(token, userDetails)) {
                logger.warn("WebSocket handshake ditolak: token tidak valid/kedaluwarsa");
                return false;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            attributes.put(ATTR_AUTHENTICATION, authentication);
            attributes.put(ATTR_USER_ID, jwtUtil.extractUserId(token));
            attributes.put(ATTR_ROLE, extractRole(userDetails));
            return true;
        } catch (Exception e) {
            logger.warn("WebSocket handshake ditolak, token tidak valid: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Tidak ada yang perlu dibersihkan di sini.
    }

    /** Diturunkan dari authority ROLE_* yang sama dipakai SecurityConfig.hasRole(...), bukan dari klaim token. */
    private String extractRole(UserDetails userDetails) {
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith("ROLE_")) {
                return name.substring("ROLE_".length());
            }
        }
        return "USER";
    }
}
