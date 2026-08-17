package com.projekfajar.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.auth.dto.GoogleLoginRequest;
import com.projekfajar.auth.dto.LoginRequest;
import com.projekfajar.auth.dto.OtpVerificationRequest;
import com.projekfajar.auth.dto.EmailVerificationRequest;
import com.projekfajar.auth.dto.RegisterRequest;
import com.projekfajar.auth.dto.ResendOtpRequest;
import com.projekfajar.auth.dto.UserLoginRequest;
import com.projekfajar.auth.service.AuthCookieService;
import com.projekfajar.auth.service.AuthService;
import com.projekfajar.exception.UnauthorizedAccessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login-admin")
    public ResponseEntity<Map<String, Object>> loginAdmin(@Valid @RequestBody LoginRequest request) {
        log.info("REST request for Admin Login: email={}", request.getEmail());
        return ResponseEntity.ok(authService.loginAdmin(request));
    }

    @PostMapping("/verifikasi-otp-admin")
    public ResponseEntity<Map<String, Object>> verifikasiOtp(
            @Valid @RequestBody OtpVerificationRequest request, HttpServletResponse response) {
        log.info("REST request to verify Admin OTP");
        return ResponseEntity.ok(withAuthCookie(authService.verifyOtpAdmin(request), response));
    }

    @PostMapping("/kirim-ulang-otp-admin")
    public ResponseEntity<Map<String, Object>> resendOtpAdmin(@Valid @RequestBody ResendOtpRequest request) {
        log.info("REST request to resend Admin OTP: email={}", request.getEmail());
        return ResponseEntity.ok(authService.resendOtpAdmin(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request for User Registration: email={}", request.getEmail());
        Map<String, Object> body = new HashMap<>(authService.register(request));
        int status = (int) body.remove("status");
        return ResponseEntity.status(HttpStatus.valueOf(status)).body(body);
    }

    /**
     * Verifikasi kepemilikan email dengan kode 6 digit.
     * Berada di bawah /auth agar ikut terlindungi AuthRateLimitFilter.
     */
    @PostMapping("/verifikasi-email")
    public ResponseEntity<Map<String, Object>> verifikasiEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        log.info("REST request to verify User Email: email={}", request.getEmail());
        return ResponseEntity.ok(
                authService.verifikasiEmail(request.getEmail(), request.getKode()));
    }

    @PostMapping("/kirim-ulang-verifikasi")
    public ResponseEntity<Map<String, Object>> kirimUlangVerifikasi(
            @Valid @RequestBody ResendOtpRequest request) {
        log.info("REST request to resend User Email verification: email={}", request.getEmail());
        return ResponseEntity.ok(authService.kirimUlangVerifikasi(request.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(
            @Valid @RequestBody UserLoginRequest request, HttpServletResponse response) {
        log.info("REST request for User Login: email={}", request.getEmail());
        return ResponseEntity.ok(withAuthCookie(authService.loginUser(request), response));
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> loginGoogle(
            @Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
        log.info("REST request for Google Login");
        try {
            return ResponseEntity.ok(withAuthCookie(authService.loginGoogle(request), response));
        } catch (UnauthorizedAccessException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    /**
     * Access JWT habis: cookie refresh (path /auth) dipakai menerbitkan
     * access + refresh baru. Gagal = 401, kedua cookie dihapus.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshMentah = authCookieService.extractRefreshToken(request);
        try {
            return ResponseEntity.ok(withAuthCookie(authService.refresh(refreshMentah), response));
        } catch (UnauthorizedAccessException ex) {
            log.info("Refresh token ditolak: {}", ex.getMessage());
            authCookieService.hapusSemua(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication,
            HttpServletRequest request, HttpServletResponse response) {
        log.info("REST request for User Logout: user={}", authentication != null ? authentication.getName() : "anonymous");
        authService.cabutRefresh(authCookieService.extractRefreshToken(request));
        authCookieService.hapusSemua(response);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logout berhasil"));
    }

    /**
     * AuthService mengembalikan token di Map — service itu tidak tahu HTTP.
     * Di sini access dan refresh dipindah ke cookie httpOnly, tidak ke JSON.
     * Login admin yang masih menunggu OTP tidak membawa token.
     */
    private Map<String, Object> withAuthCookie(Map<String, Object> body, HttpServletResponse response) {
        Object token = body.get("token");
        Object refresh = body.get("refreshToken");
        if (token == null) {
            return body;
        }

        authCookieService.pasang(response, token.toString(),
                refresh != null ? refresh.toString() : null);

        Map<String, Object> withoutToken = new HashMap<>(body);
        withoutToken.remove("token");
        withoutToken.remove("refreshToken");
        return withoutToken;
    }
}
