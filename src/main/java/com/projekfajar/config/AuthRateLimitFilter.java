package com.projekfajar.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Membatasi jumlah percobaan pada endpoint autentikasi.
 *
 * Tanpa ini, login dan verifikasi OTP bisa dicoba tanpa batas dari satu alamat
 * IP — cukup untuk menebak password lemah atau OTP 6 digit secara beruntun.
 * Penghitung disimpan di memori: cukup untuk satu instance aplikasi. Bila nanti
 * berjalan di banyak instance, pindahkan ke Redis agar batasnya berlaku bersama.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    @Value("${app.rate-limit.auth.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.rate-limit.auth.window-seconds:60}")
    private long windowSeconds;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private static final class Attempt {
        private final AtomicInteger count = new AtomicInteger();
        private volatile Instant windowStart = Instant.now();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/auth/")) {
            return true;
        }
        // Refresh/logout bukan tebakan password — jangan ikut kuota login.
        return uri.equals("/auth/refresh") || uri.equals("/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = clientKey(request);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());

        synchronized (attempt) {
            Instant now = Instant.now();

            if (Duration.between(attempt.windowStart, now).getSeconds() >= windowSeconds) {
                attempt.windowStart = now;
                attempt.count.set(0);
            }

            if (attempt.count.incrementAndGet() > maxAttempts) {
                logger.warn("Rate limit tercapai untuk {} pada {}", key, request.getRequestURI());

                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Terlalu banyak percobaan. Coba lagi dalam beberapa saat.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);

        // Jaga agar peta tidak tumbuh tanpa batas pada instance yang lama hidup
        if (attempts.size() > 10_000) {
            attempts.clear();
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
