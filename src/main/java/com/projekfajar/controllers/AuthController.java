package com.projekfajar.controllers;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.DTO.LoginRequest;
import com.projekfajar.DTO.OtpVerificationRequest;
import com.projekfajar.DTO.RegisterRequest;
import com.projekfajar.DTO.ResendOtpRequest;
import com.projekfajar.DTO.UserLoginRequest;
import com.projekfajar.exception.EmailNotFoundException;
import com.projekfajar.exception.EmailSendException;
import com.projekfajar.exception.InvalidOtpException;
import com.projekfajar.exception.InvalidPasswordException;
import com.projekfajar.exception.OtpExpiredException;
import com.projekfajar.exception.UnauthorizedAccessException;
import com.projekfajar.models.LoginOtp;
import com.projekfajar.models.Role;
import com.projekfajar.models.User;
import com.projekfajar.repository.LoginOtpRepository;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.EmailService;
import com.projekfajar.util.JwtUtil;
import com.projekfajar.util.OtpGenerator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final LoginOtpRepository loginOtpRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final com.projekfajar.services.NotificationService notificationService;

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for email: {}", loginRequest.getEmail());
            
            // Validate email exists
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

            // Validate password
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                logger.error("User password is null or empty for email: {}", loginRequest.getEmail());
                throw new InvalidPasswordException("Data pengguna tidak valid");
            }

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                logger.warn("Invalid password attempt for email: {}", loginRequest.getEmail());
                throw new InvalidPasswordException("Password salah");
            }

            // Validate admin role
            if (user.getRole() == null || user.getRole() != Role.ADMIN) {
                logger.warn("Non-admin access attempt for email: {}", loginRequest.getEmail());
                throw new UnauthorizedAccessException("Akses khusus admin");
            }

            // Generate and save OTP
            String otp = otpGenerator.generate4Digit();
            if (otp == null || otp.trim().isEmpty()) {
                logger.error("Failed to generate OTP");
                throw new RuntimeException("Gagal menghasilkan kode OTP");
            }

            LoginOtp loginOtp = LoginOtp.builder()
                    .otp(otp)
                    .user(user)
                    .expiredAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            try {
                loginOtpRepository.save(loginOtp);
                logger.info("OTP saved successfully for user: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to save OTP: {}", e.getMessage());
                throw new RuntimeException("Gagal menyimpan kode OTP");
            }

            // Send OTP via email
            try {
                emailService.sendOtp(user.getEmail(), otp);
                logger.info("OTP sent successfully to email: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send OTP email: {}", e.getMessage());
                throw new EmailSendException("Gagal mengirim kode OTP ke email", e);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Kode OTP dikirim ke email",
                    "success", true));
                    
        } catch (EmailNotFoundException | InvalidPasswordException | UnauthorizedAccessException | EmailSendException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during login: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan sistem, silakan coba lagi");
        }
    }

    @PostMapping("/verifikasi-otp-admin")
    public ResponseEntity<?> verifikasiOtp(@Valid @RequestBody OtpVerificationRequest request) {
        try {
            String kode = request.getKode().trim();
            logger.info("OTP verification attempt with code length: {}", kode.length());

            // Validate OTP format (additional check)
            if (!kode.matches("^[0-9]{4}$")) {
                logger.warn("Invalid OTP format: {}", kode);
                throw new InvalidOtpException("Format kode OTP tidak valid");
            }

            // Find OTP
            LoginOtp otp = loginOtpRepository
                    .findByOtpAndUsedFalse(kode)
                    .orElseThrow(() -> {
                        logger.warn("Invalid or already used OTP: {}", kode);
                        return new InvalidOtpException("Kode OTP tidak valid atau sudah digunakan");
                    });

            // Check if OTP expired
            if (otp.getExpiredAt() == null) {
                logger.error("OTP expiration time is null");
                throw new InvalidOtpException("Data OTP tidak valid");
            }

            if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
                logger.warn("Expired OTP attempt: {}", kode);
                throw new OtpExpiredException("Kode OTP kadaluarsa, silakan minta kode baru");
            }

            // Validate user exists
            if (otp.getUser() == null) {
                logger.error("OTP has no associated user");
                throw new InvalidOtpException("Data OTP tidak valid");
            }

            // Mark OTP as used
            try {
                otp.setUsed(true);
                loginOtpRepository.save(otp);
                logger.info("OTP marked as used for user: {}", otp.getUser().getEmail());
            } catch (Exception e) {
                logger.error("Failed to mark OTP as used: {}", e.getMessage());
                throw new RuntimeException("Gagal memproses verifikasi OTP");
            }

            // Update last login time for admin
            User admin = otp.getUser();
            admin.setLastLogin(LocalDateTime.now());
            userRepository.save(admin);

            // Generate JWT
            String jwt;
            try {
                jwt = jwtUtil.generateToken(admin);
                logger.info("JWT token generated successfully for user: {}", admin.getEmail());
            } catch (Exception e) {
                logger.error("Failed to generate JWT token: {}", e.getMessage());
                throw new RuntimeException("Gagal menghasilkan token autentikasi");
            }

            // Validate user data
            if (admin.getNamaLengkap() == null || admin.getNamaLengkap().trim().isEmpty()) {
                logger.warn("User has no name: {}", admin.getEmail());
            }

            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "namaLengkap", admin.getNamaLengkap() != null ? admin.getNamaLengkap() : "",
                    "email", admin.getEmail(),
                    "role", admin.getRole().toString(),
                    "success", true));
                    
        } catch (InvalidOtpException | OtpExpiredException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during OTP verification: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan sistem, silakan coba lagi");
        }
    }

    @PostMapping("/kirim-ulang-otp-admin")
    public ResponseEntity<?> resendOtpAdmin(@Valid @RequestBody ResendOtpRequest request) {
        try {
            logger.info("Resend OTP request for email: {}", request.getEmail());
            
            // Validate email exists
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

            // Validate admin role
            if (user.getRole() == null || user.getRole() != Role.ADMIN) {
                logger.warn("Non-admin resend OTP attempt for email: {}", request.getEmail());
                throw new UnauthorizedAccessException("Akses khusus admin");
            }

            // Mark all previous unused OTPs as used (expired)
            try {
                loginOtpRepository.findByUserAndUsedFalse(user)
                        .forEach(oldOtp -> {
                            oldOtp.setUsed(true);
                            loginOtpRepository.save(oldOtp);
                        });
                logger.info("Previous OTPs marked as used for user: {}", user.getEmail());
            } catch (Exception e) {
                logger.warn("Error marking old OTPs as used: {}", e.getMessage());
            }

            // Generate new OTP
            String otp = otpGenerator.generate4Digit();
            if (otp == null || otp.trim().isEmpty()) {
                logger.error("Failed to generate OTP");
                throw new RuntimeException("Gagal menghasilkan kode OTP");
            }

            LoginOtp loginOtp = LoginOtp.builder()
                    .otp(otp)
                    .user(user)
                    .expiredAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            try {
                loginOtpRepository.save(loginOtp);
                logger.info("New OTP saved successfully for user: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to save new OTP: {}", e.getMessage());
                throw new RuntimeException("Gagal menyimpan kode OTP");
            }

            // Send OTP via email
            try {
                emailService.sendOtp(user.getEmail(), otp);
                logger.info("Resent OTP sent successfully to email: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send resent OTP email: {}", e.getMessage());
                throw new EmailSendException("Gagal mengirim ulang kode OTP ke email", e);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Kode OTP baru telah dikirim ke email",
                    "success", true));
                    
        } catch (EmailNotFoundException | UnauthorizedAccessException | EmailSendException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during resend OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan sistem, silakan coba lagi");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            logger.info("Register attempt for email: {}", request.getEmail());

            // Check if email already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email sudah terdaftar"));
            }

            // Create new user
            User user = User.builder()
                    .namaLengkap(request.getNamaLengkap())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .noHp(request.getNoHp())
                    .role(Role.USER)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(user);
            logger.info("User registered successfully: {}", user.getEmail());
            
            // Send admin notification about new customer
            try {
                notificationService.sendNewCustomerNotification(user);
            } catch (Exception e) {
                logger.error("Failed to send new customer notification: {}", e.getMessage(), e);
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Registrasi berhasil, silakan login"));
            
        } catch (Exception e) {
            logger.error("Error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Terjadi kesalahan sistem, silakan coba lagi"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginRequest request) {
        try {
            logger.info("User login attempt for email: {}", request.getEmail());

            // Validate email exists
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new EmailNotFoundException("Email tidak terdaftar"));

            // Validate password
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                logger.error("User password is null or empty for email: {}", request.getEmail());
                throw new InvalidPasswordException("Data pengguna tidak valid");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.warn("Invalid password attempt for email: {}", request.getEmail());
                throw new InvalidPasswordException("Password salah");
            }

            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT directly for regular users (no OTP)
            String jwt = jwtUtil.generateToken(user);
            logger.info("User logged in successfully: {}", user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login berhasil",
                    "token", jwt,
                    "user", Map.of(
                            "id", user.getId(),
                            "namaLengkap", user.getNamaLengkap(),
                            "email", user.getEmail(),
                            "noHp", user.getNoHp() != null ? user.getNoHp() : "",
                            "role", user.getRole().toString())));

        } catch (EmailNotFoundException | InvalidPasswordException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during user login: {}", e.getMessage(), e);
            throw new RuntimeException("Terjadi kesalahan sistem, silakan coba lagi");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        try {
            if (authentication != null) {
                logger.info("User logout: {}", authentication.getName());
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout berhasil"));
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout berhasil"));
        }
    }
}
