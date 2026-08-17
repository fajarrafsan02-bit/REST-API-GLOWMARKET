package com.projekfajar.auth.service;

import com.projekfajar.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.auth.dto.GoogleLoginRequest;
import com.projekfajar.auth.dto.LoginRequest;
import com.projekfajar.auth.dto.OtpVerificationRequest;
import com.projekfajar.auth.dto.RegisterRequest;
import com.projekfajar.auth.dto.ResendOtpRequest;
import com.projekfajar.auth.dto.UserLoginRequest;
import com.projekfajar.exception.EmailNotFoundException;
import com.projekfajar.exception.EmailSendException;
import com.projekfajar.exception.InvalidOtpException;
import com.projekfajar.exception.InvalidPasswordException;
import com.projekfajar.exception.OtpExpiredException;
import com.projekfajar.exception.UnauthorizedAccessException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.projekfajar.auth.model.EmailVerification;
import com.projekfajar.auth.model.LoginOtp;
import com.projekfajar.auth.model.Role;
import com.projekfajar.user.model.User;
import com.projekfajar.auth.repository.EmailVerificationRepository;
import com.projekfajar.auth.repository.LoginOtpRepository;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.util.JwtUtil;
import com.projekfajar.util.OtpGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final LoginOtpRepository loginOtpRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenService refreshTokenService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    /** Masa berlaku kode verifikasi email. */
    private static final int VERIFIKASI_BERLAKU_MENIT = 15;

    /** Batas salah kode sebelum kode dibatalkan — 6 digit terlalu mudah ditebak tanpa ini. */
    private static final int VERIFIKASI_MAKS_PERCOBAAN = 5;

    public Map<String, Object> loginAdmin(LoginRequest request) {
        log.info("Memproses percobaan login Admin untuk email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login Admin gagal: Email {} tidak terdaftar", request.getEmail());
                    return new EmailNotFoundException("Email tidak terdaftar");
                });

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            log.error("Data password kosong untuk email: {}", request.getEmail());
            throw new InvalidPasswordException("Data pengguna tidak valid");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Password salah saat login Admin untuk email: {}", request.getEmail());
            throw new InvalidPasswordException("Password salah");
        }

        if (user.getRole() == null || user.getRole() != Role.ADMIN) {
            log.warn("Percobaan akses Admin oleh non-admin: email={}, role={}", request.getEmail(), user.getRole());
            throw new UnauthorizedAccessException("Akses khusus admin");
        }

        return terbitkanOtpAdmin(user);
    }

    /**
     * Membuat, menyimpan, dan mengirim OTP login admin.
     *
     * Dipakai bersama oleh login admin biasa (email + password) dan login
     * Google untuk akun admin — keduanya sama-sama wajib melewati verifikasi
     * dua langkah, apa pun cara masuknya.
     */
    private Map<String, Object> terbitkanOtpAdmin(User user) {
        String otp = otpGenerator.generate4Digit();
        if (otp == null || otp.trim().isEmpty()) {
            log.error("Gagal membuat OTP untuk email: {}", user.getEmail());
            throw new RuntimeException("Gagal menghasilkan kode OTP");
        }

        LoginOtp loginOtp = LoginOtp.builder()
                .otp(otp)
                .user(user)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        try {
            loginOtpRepository.save(loginOtp);
            log.info("Kode OTP Admin berhasil disimpan untuk user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Gagal menyimpan OTP ke database: {}", e.getMessage(), e);
            throw new RuntimeException("Gagal menyimpan kode OTP");
        }

        try {
            emailService.sendOtp(user.getEmail(), otp);
            log.info("OTP Admin berhasil dikirim ke email: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Gagal mengirim email OTP Admin ke {}: {}", user.getEmail(), e.getMessage());
            throw new EmailSendException("Gagal mengirim kode OTP ke email", e);
        }

        return Map.of(
                "message", "Kode OTP dikirim ke email",
                "success", true);
    }

    public Map<String, Object> verifyOtpAdmin(OtpVerificationRequest request) {
        String kode = request.getKode().trim();
        log.info("Verifikasi OTP Admin diproses: kodeLength={}", kode.length());

        if (!kode.matches("^[0-9]{4}$")) {
            log.warn("Format OTP Admin tidak valid: {}", kode);
            throw new InvalidOtpException("Format kode OTP tidak valid");
        }

        LoginOtp otp = loginOtpRepository
                .findByOtpAndUsedFalse(kode)
                .orElseThrow(() -> {
                    log.warn("Kode OTP Admin tidak valid atau sudah digunakan: {}", kode);
                    return new InvalidOtpException("Kode OTP tidak valid atau sudah digunakan");
                });

        if (otp.getExpiredAt() == null) {
            log.error("OTP expiration time is null");
            throw new InvalidOtpException("Data OTP tidak valid");
        }

        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired OTP attempt: {}", kode);
            throw new OtpExpiredException("Kode OTP kadaluarsa, silakan minta kode baru");
        }

        if (otp.getUser() == null) {
            log.error("OTP has no associated user");
            throw new InvalidOtpException("Data OTP tidak valid");
        }

        try {
            otp.setUsed(true);
            loginOtpRepository.save(otp);
            log.info("OTP marked as used for user: {}", otp.getUser().getEmail());
        } catch (Exception e) {
            log.error("Failed to mark OTP as used: {}", e.getMessage());
            throw new RuntimeException("Gagal memproses verifikasi OTP");
        }

        User admin = otp.getUser();
        admin.setLastLogin(LocalDateTime.now());
        userRepository.save(admin);

        if (admin.getNamaLengkap() == null || admin.getNamaLengkap().trim().isEmpty()) {
            log.warn("User has no name: {}", admin.getEmail());
        }

        log.info("JWT token generated successfully for user: {}", admin.getEmail());
        return sesi(admin, Map.of(
                "namaLengkap", admin.getNamaLengkap() != null ? admin.getNamaLengkap() : "",
                "email", admin.getEmail(),
                "role", admin.getRole().toString(),
                "success", true));
    }

    public Map<String, Object> resendOtpAdmin(ResendOtpRequest request) {
        log.info("Resend OTP request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

        if (user.getRole() == null || user.getRole() != Role.ADMIN) {
            log.warn("Non-admin resend OTP attempt for email: {}", request.getEmail());
            throw new UnauthorizedAccessException("Akses khusus admin");
        }

        try {
            loginOtpRepository.findByUserAndUsedFalse(user)
                    .forEach(oldOtp -> {
                        oldOtp.setUsed(true);
                        loginOtpRepository.save(oldOtp);
                    });
            log.info("Previous OTPs marked as used for user: {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Error marking old OTPs as used: {}", e.getMessage());
        }

        String otp = otpGenerator.generate4Digit();
        if (otp == null || otp.trim().isEmpty()) {
            log.error("Failed to generate OTP");
            throw new RuntimeException("Gagal menghasilkan kode OTP");
        }

        LoginOtp loginOtp = LoginOtp.builder()
                .otp(otp)
                .user(user)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        try {
            loginOtpRepository.save(loginOtp);
            log.info("New OTP saved successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to save new OTP: {}", e.getMessage());
            throw new RuntimeException("Gagal menyimpan kode OTP");
        }

        try {
            emailService.sendOtp(user.getEmail(), otp);
            log.info("Resent OTP sent successfully to email: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send resent OTP email: {}", e.getMessage());
            throw new EmailSendException("Gagal mengirim ulang kode OTP ke email", e);
        }

        return Map.of(
                "message", "Kode OTP baru telah dikirim ke email",
                "success", true);
    }

    public Map<String, Object> register(RegisterRequest request) {
        log.info("Register attempt for email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return Map.of("success", false, "status", 400, "message", "Email sudah terdaftar");
        }

        try {
            User user = User.builder()
                    .namaLengkap(request.getNamaLengkap())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .noHp(request.getNoHp())
                    .role(Role.USER)
                    // Akun aktif (tidak diblokir admin), tetapi emailnya belum terbukti
                    .terverifikasi(true)
                    .emailTerverifikasi(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(user);
            log.info("User registered successfully: {}", user.getEmail());

            try {
                notificationService.sendNewCustomerNotification(user);
            } catch (Exception e) {
                log.error("Failed to send new customer notification: {}", e.getMessage(), e);
            }

            terbitkanKodeVerifikasi(user);

            return Map.of(
                    "success", true,
                    "status", 200,
                    "message", "Registrasi berhasil. Kami mengirim kode verifikasi ke " + user.getEmail(),
                    "butuhVerifikasiEmail", true,
                    "email", user.getEmail());
        } catch (Exception e) {
            log.error("Error during registration: {}", e.getMessage(), e);
            return Map.of("success", false, "status", 500, "message", "Terjadi kesalahan sistem, silakan coba lagi");
        }
    }

    /**
     * Membuat kode verifikasi baru dan mengirimkannya.
     *
     * Kode lama dibatalkan agar hanya satu kode yang berlaku pada satu waktu.
     * Kegagalan SMTP tidak membatalkan akun — pengguna bisa meminta kirim ulang.
     */
    private void terbitkanKodeVerifikasi(User user) {
        emailVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .ifPresent(lama -> {
                    lama.setUsed(true);
                    emailVerificationRepository.save(lama);
                });

        String kode = otpGenerator.generate6Digit();

        emailVerificationRepository.save(EmailVerification.builder()
                .user(user)
                .kode(kode)
                .expiredAt(LocalDateTime.now().plusMinutes(VERIFIKASI_BERLAKU_MENIT))
                .createdAt(LocalDateTime.now())
                .build());

        try {
            emailService.sendEmailVerification(user.getEmail(), user.getNamaLengkap(), kode);
        } catch (EmailSendException e) {
            log.error("Gagal mengirim kode verifikasi ke {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public Map<String, Object> verifikasiEmail(String email, String kode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

        if (Boolean.TRUE.equals(user.getEmailTerverifikasi())) {
            return Map.of("success", true, "message", "Email sudah terverifikasi");
        }

        EmailVerification verifikasi = emailVerificationRepository
                .findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException(
                        "Tidak ada kode aktif. Silakan minta kode baru."));

        if (verifikasi.kedaluwarsa()) {
            throw new OtpExpiredException("Kode sudah kedaluwarsa. Silakan minta kode baru.");
        }

        if (!verifikasi.getKode().equals(kode)) {
            verifikasi.setPercobaan(verifikasi.getPercobaan() + 1);

            if (verifikasi.getPercobaan() >= VERIFIKASI_MAKS_PERCOBAAN) {
                verifikasi.setUsed(true);
                emailVerificationRepository.save(verifikasi);
                throw new InvalidOtpException(
                        "Kode salah terlalu sering. Kode dibatalkan, silakan minta kode baru.");
            }

            emailVerificationRepository.save(verifikasi);
            throw new InvalidOtpException("Kode verifikasi salah. Sisa percobaan: "
                    + (VERIFIKASI_MAKS_PERCOBAAN - verifikasi.getPercobaan()));
        }

        verifikasi.setUsed(true);
        emailVerificationRepository.save(verifikasi);

        user.setEmailTerverifikasi(true);
        user.setEmailTerverifikasiAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Email terverifikasi untuk: {}", user.getEmail());
        return Map.of("success", true, "message", "Email berhasil diverifikasi");
    }

    public Map<String, Object> kirimUlangVerifikasi(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

        if (Boolean.TRUE.equals(user.getEmailTerverifikasi())) {
            return Map.of("success", true, "message", "Email sudah terverifikasi");
        }

        terbitkanKodeVerifikasi(user);

        return Map.of("success", true, "message", "Kode verifikasi baru sudah dikirim ke " + email);
    }

    public Map<String, Object> loginUser(UserLoginRequest request) {
        log.info("User login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            log.error("User password is null or empty for email: {}", request.getEmail());
            throw new InvalidPasswordException("Data pengguna tidak valid");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for email: {}", request.getEmail());
            throw new InvalidPasswordException("Password salah");
        }

        if (Boolean.FALSE.equals(user.getTerverifikasi())) {
            log.warn("Deactivated user login attempt for email: {}", request.getEmail());
            throw new UnauthorizedAccessException("Akun Anda telah dinonaktifkan oleh Admin. Silakan hubungi customer service.");
        }

        // Jika role pengakses adalah ADMIN, wajib alihkan ke alur OTP Admin
        if (user.getRole() == Role.ADMIN) {
            log.info("Admin login detected in loginUser for email: {}. Triggering OTP...", user.getEmail());
            LoginRequest loginReq = new LoginRequest();
            loginReq.setEmail(user.getEmail());
            loginReq.setPassword(request.getPassword());
            return loginAdmin(loginReq);
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getEmail());
        return sesi(user, Map.of(
                "success", true,
                "message", "Login berhasil",
                "user", Map.of(
                        "id", user.getId(),
                        "namaLengkap", user.getNamaLengkap(),
                        "email", user.getEmail(),
                        "noHp", user.getNoHp() != null ? user.getNoHp() : "",
                        "role", user.getRole().toString())));
    }

    /**
     * Login/daftar otomatis lewat akun Google. ID token diverifikasi ke
     * Google (tanda tangan, audience, kedaluwarsa) sebelum data profil di
     * dalamnya dipercaya — sesuai keputusan: email yang sama dengan akun
     * yang sudah terdaftar manual otomatis digabung (dianggap orang yang
     * sama), bukan ditolak atau dibuatkan akun baru.
     */
    @Transactional
    public Map<String, Object> loginGoogle(GoogleLoginRequest request) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(request.getCredential());
        } catch (Exception e) {
            log.warn("Gagal memverifikasi token Google: {}", e.getMessage());
            throw new UnauthorizedAccessException("Verifikasi Google gagal");
        }

        if (idToken == null) {
            log.warn("Token Google tidak valid (verify() mengembalikan null)");
            throw new UnauthorizedAccessException("Token Google tidak valid");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String nama = (String) payload.get("name");

        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            log.warn("Login Google ditolak: email tidak ada/tidak terverifikasi di sisi Google");
            throw new UnauthorizedAccessException("Email Google belum terverifikasi");
        }

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        if (user == null) {
            user = User.builder()
                    .namaLengkap(nama != null ? nama : email)
                    .email(email)
                    .googleId(googleId)
                    .role(Role.USER)
                    .terverifikasi(true)
                    .emailTerverifikasi(true)
                    .emailTerverifikasiAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            log.info("Akun baru dibuat lewat Google: {}", email);

            try {
                notificationService.sendNewCustomerNotification(user);
            } catch (Exception e) {
                log.error("Failed to send new customer notification: {}", e.getMessage(), e);
            }
        } else {
            boolean berubah = false;
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                berubah = true;
            }
            // Email lewat Google sudah pasti terverifikasi Google sendiri —
            // akun lama yang belum verifikasi email manual ikut terangkat.
            if (Boolean.FALSE.equals(user.getEmailTerverifikasi())) {
                user.setEmailTerverifikasi(true);
                user.setEmailTerverifikasiAt(LocalDateTime.now());
                berubah = true;
            }
            if (berubah) {
                userRepository.save(user);
            }
        }

        if (Boolean.FALSE.equals(user.getTerverifikasi())) {
            log.warn("Login Google ditolak, akun dinonaktifkan admin: {}", email);
            throw new UnauthorizedAccessException(
                    "Akun Anda telah dinonaktifkan oleh Admin. Silakan hubungi customer service.");
        }

        /*
         * Admin boleh masuk lewat Google, tetapi tetap wajib melewati OTP
         * email seperti login admin biasa — Google hanya menggantikan
         * langkah "email + password", bukan verifikasi dua langkahnya.
         * Respons tanpa token membuat frontend berpindah ke layar OTP.
         */
        if (user.getRole() == Role.ADMIN) {
            log.info("Login Google oleh akun admin {}, menerbitkan OTP", email);
            Map<String, Object> hasil = new HashMap<>(terbitkanOtpAdmin(user));
            hasil.put("email", user.getEmail());
            hasil.put("butuhOtpAdmin", true);
            return hasil;
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login Google berhasil: {}", email);
        return sesi(user, Map.of(
                "success", true,
                "message", "Login berhasil",
                "user", Map.of(
                        "id", user.getId(),
                        "namaLengkap", user.getNamaLengkap(),
                        "email", user.getEmail(),
                        "noHp", user.getNoHp() != null ? user.getNoHp() : "",
                        "role", user.getRole().toString())));
    }

    @Transactional
    public Map<String, Object> refresh(String refreshMentah) {
        User user = refreshTokenService.tukar(refreshMentah);
        if (Boolean.FALSE.equals(user.getTerverifikasi())) {
            throw new UnauthorizedAccessException(
                    "Akun Anda telah dinonaktifkan oleh Admin. Silakan hubungi customer service.");
        }
        log.info("Access token diperbarui untuk {}", user.getEmail());
        return sesi(user, Map.of("success", true, "message", "Sesi diperbarui"));
    }

    public void cabutRefresh(String refreshMentah) {
        refreshTokenService.cabut(refreshMentah);
    }

    private Map<String, Object> sesi(User user, Map<String, Object> body) {
        Map<String, Object> hasil = new HashMap<>(body);
        hasil.put("token", jwtUtil.generateToken(user));
        hasil.put("refreshToken", refreshTokenService.terbitkan(user));
        return hasil;
    }
}
