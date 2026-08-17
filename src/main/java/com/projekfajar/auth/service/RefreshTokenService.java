package com.projekfajar.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.auth.model.RefreshToken;
import com.projekfajar.auth.repository.RefreshTokenRepository;
import com.projekfajar.exception.UnauthorizedAccessException;
import com.projekfajar.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh token opaqe — bukan JWT. Nilai mentah hanya ada di cookie httpOnly;
 * di database yang disimpan hash-nya, jadi mencuri isi tabel tidak cukup untuk
 * menyamar sebagai pengguna.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Transactional
    public String terbitkan(User user) {
        String mentah = buatNilaiAcak();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(mentah))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build());
        log.debug("Refresh token diterbitkan untuk user {}", user.getEmail());
        return mentah;
    }

    /**
     * Rotasi: token lama dicabut. Pemanggil wajib menerbitkan token baru
     * lewat {@link #terbitkan(User)}.
     */
    @Transactional
    public User tukar(String refreshMentah) {
        if (refreshMentah == null || refreshMentah.isBlank()) {
            throw new UnauthorizedAccessException("Refresh token tidak valid");
        }

        RefreshToken baris = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(refreshMentah))
                .orElseThrow(() -> new UnauthorizedAccessException("Refresh token tidak valid"));

        if (baris.getExpiresAt().isBefore(LocalDateTime.now())) {
            baris.setRevoked(true);
            refreshTokenRepository.save(baris);
            throw new UnauthorizedAccessException("Refresh token sudah kedaluwarsa");
        }

        baris.setRevoked(true);
        refreshTokenRepository.save(baris);
        log.debug("Refresh token dirotasi untuk user {}", baris.getUser().getEmail());
        return baris.getUser();
    }

    @Transactional
    public void cabut(String refreshMentah) {
        if (refreshMentah == null || refreshMentah.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash(refreshMentah))
                .ifPresent(baris -> {
                    baris.setRevoked(true);
                    refreshTokenRepository.save(baris);
                });
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private static String buatNilaiAcak() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String mentah) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(mentah.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tidak tersedia", e);
        }
    }
}
