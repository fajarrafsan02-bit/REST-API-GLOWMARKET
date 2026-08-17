package com.projekfajar.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Satu-satunya tempat yang tahu bentuk cookie autentikasi (nama, umur,
 * path, atribut keamanan) — access JWT di path / dan refresh token hanya
 * di path /auth supaya tidak ikut ke setiap request API.
 */
@Component
@Slf4j
public class AuthCookieService {

    private static final String PATH_ACCESS = "/";
    private static final String PATH_REFRESH = "/auth";

    @Value("${app.auth.cookie-name:auth_token}")
    private String cookieName;

    @Value("${app.auth.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;

    /**
     * Lax cukup selama frontend dan backend satu origin (dev memakai proxy
     * Vite). Begitu keduanya beda domain — misalnya frontend di Vercel dan
     * backend di Render — browser menganggap setiap request sebagai
     * lintas-situs dan menahan cookie Lax, sehingga pengguna tampak tidak
     * pernah berhasil login. Untuk kasus itu setel None, yang mensyaratkan
     * Secure=true (HTTPS).
     */
    @Value("${app.auth.cookie-same-site:Lax}")
    private String cookieSameSite;

    @Value("${jwt.access-expiration-ms:1800000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    public String getCookieName() {
        return cookieName;
    }

    public ResponseCookie build(String jwt) {
        return accessCookie(jwt, Duration.ofMillis(accessExpirationMs));
    }

    public ResponseCookie buildRefresh(String refreshMentah) {
        return refreshCookie(refreshMentah, Duration.ofMillis(refreshExpirationMs));
    }

    public ResponseCookie clear() {
        return accessCookie("", Duration.ZERO);
    }

    public ResponseCookie clearRefresh() {
        return refreshCookie("", Duration.ZERO);
    }

    public void pasang(HttpServletResponse response, String accessJwt, String refreshMentah) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(accessJwt).toString());
        if (refreshMentah != null && !refreshMentah.isBlank()) {
            response.addHeader(HttpHeaders.SET_COOKIE, buildRefresh(refreshMentah).toString());
        }
    }

    public void hapusSemua(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, clear().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh().toString());
    }

    public void hapusAccessSaja(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, clear().toString());
    }

    public String extractToken(HttpServletRequest request) {
        return bacaCookie(request, cookieName);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return bacaCookie(request, refreshCookieName);
    }

    private ResponseCookie accessCookie(String nilai, Duration maxAge) {
        return ResponseCookie.from(cookieName, nilai == null ? "" : nilai)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(PATH_ACCESS)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie refreshCookie(String nilai, Duration maxAge) {
        return ResponseCookie.from(refreshCookieName, nilai == null ? "" : nilai)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(PATH_REFRESH)
                .maxAge(maxAge)
                .build();
    }

    private static String bacaCookie(HttpServletRequest request, String nama) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (nama.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
