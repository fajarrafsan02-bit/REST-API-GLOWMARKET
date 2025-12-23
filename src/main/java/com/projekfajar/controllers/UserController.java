package com.projekfajar.controllers;

import com.projekfajar.DTO.UpdateProfilRequest;
import com.projekfajar.DTO.UserResponse;
import com.projekfajar.DTO.CustomerResponse;
import com.projekfajar.models.User;
import com.projekfajar.models.Role;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.repository.PesananRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PesananRepository pesananRepository;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            UserResponse profile = UserResponse.builder()
                    .id(user.getId())
                    .namaLengkap(user.getNamaLengkap())
                    .email(user.getEmail())
                    .noHp(user.getNoHp())
                    .role(user.getRole())
                    .terferifikasi(user.getTerferifikasi())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil berhasil diambil",
                    "data", profile));
        } catch (Exception e) {
            logger.error("Error getting profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/admin-profile")
    public ResponseEntity<Map<String, Object>> getAdminProfile(Authentication authentication) {
        try {
            logger.info("Getting admin profile, authentication: {}", authentication != null ? authentication.getName() : "null");
            
            if (authentication == null) {
                logger.warn("No authentication provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            logger.info("Email from authentication: {}", email);
            
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));
            
            logger.info("User found: {}, role: {}", admin.getEmail(), admin.getRole());
            
            // Only admin can access this
            if (admin.getRole() != Role.ADMIN) {
                logger.warn("User {} is not an admin, role: {}", email, admin.getRole());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }

            UserResponse profile = UserResponse.builder()
                    .id(admin.getId())
                    .namaLengkap(admin.getNamaLengkap())
                    .email(admin.getEmail())
                    .noHp(admin.getNoHp())
                    .role(admin.getRole())
                    .terferifikasi(admin.getTerferifikasi())
                    .createdAt(admin.getCreatedAt())
                    .lastLogin(admin.getLastLogin())
                    .build();

            logger.info("Admin profile retrieved successfully for: {}", email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil admin berhasil diambil",
                    "data", profile));
        } catch (Exception e) {
            logger.error("Error getting admin profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/total-pelanggan")
    public ResponseEntity<Map<String, Object>> getTotalPelanggan(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access this
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            long totalPelanggan = userRepository.countByRole(com.projekfajar.models.Role.USER);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Total pelanggan berhasil diambil",
                    "data", Map.of("total", totalPelanggan)));
        } catch (Exception e) {
            logger.error("Error getting total pelanggan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCustomers(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access this
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            // Get all users with role USER
            var customers = userRepository.findByRole(Role.USER).stream()
                    .map(customer -> {
                        try {
                            // Count total orders
                            Long totalOrders = pesananRepository.countByUserId(customer.getId());
                            if (totalOrders == null) totalOrders = 0L;
                            
                            // Calculate total spent
                            Double totalSpent = 0.0;
                            try {
                                totalSpent = pesananRepository.findByUserId(customer.getId()).stream()
                                        .mapToDouble(pesanan -> pesanan.getTotalHarga() != null ? pesanan.getTotalHarga() : 0.0)
                                        .sum();
                            } catch (Exception e) {
                                logger.warn("Error calculating total spent for user {}: {}", customer.getId(), e.getMessage());
                            }
                            
                            return CustomerResponse.builder()
                                    .id(customer.getId())
                                    .nama(customer.getNamaLengkap())
                                    .email(customer.getEmail())
                                    .phone(customer.getNoHp())
                                    .role(customer.getRole())
                                    .isActive(customer.getTerferifikasi() != null ? customer.getTerferifikasi() : false)
                                    .createdAt(customer.getCreatedAt())
                                    .lastLogin(customer.getLastLogin())
                                    .totalOrders(totalOrders)
                                    .totalSpent(totalSpent)
                                    .build();
                        } catch (Exception e) {
                            logger.error("Error processing customer {}: {}", customer.getId(), e.getMessage(), e);
                            // Return basic info on error
                            return CustomerResponse.builder()
                                    .id(customer.getId())
                                    .nama(customer.getNamaLengkap())
                                    .email(customer.getEmail())
                                    .phone(customer.getNoHp())
                                    .role(customer.getRole())
                                    .isActive(false)
                                    .createdAt(customer.getCreatedAt())
                                    .lastLogin(null)
                                    .totalOrders(0L)
                                    .totalSpent(0.0)
                                    .build();
                        }
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data pelanggan berhasil diambil",
                    "data", customers));
        } catch (Exception e) {
            logger.error("Error getting all customers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat data: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody UpdateProfilRequest request,
            Authentication authentication) {
                System.out.println("test");
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Update nama lengkap
            if (request.getNamaLengkap() != null && !request.getNamaLengkap().isEmpty()) {
                user.setNamaLengkap(request.getNamaLengkap());
            }

            // Update nomor HP
            if (request.getNoHp() != null && !request.getNoHp().isEmpty()) {
                user.setNoHp(request.getNoHp());
            }

            // Update password jika diminta
            if (request.getPasswordBaru() != null && !request.getPasswordBaru().isEmpty()) {
                if (request.getPasswordLama() == null || request.getPasswordLama().isEmpty()) {
                    throw new RuntimeException("Password lama harus diisi");
                }

                // Verify old password
                if (!passwordEncoder.matches(request.getPasswordLama(), user.getPassword())) {
                    throw new RuntimeException("Password lama tidak sesuai");
                }

                // Validate new password
                if (request.getPasswordBaru().length() < 6) {
                    throw new RuntimeException("Password baru minimal 6 karakter");
                }

                user.setPassword(passwordEncoder.encode(request.getPasswordBaru()));
            }

            User updated = userRepository.save(user);

            UserResponse response = UserResponse.builder()
                    .id(updated.getId())
                    .namaLengkap(updated.getNamaLengkap())
                    .email(updated.getEmail())
                    .noHp(updated.getNoHp())
                    .role(updated.getRole())
                    .terferifikasi(updated.getTerferifikasi())
                    .createdAt(updated.getCreatedAt())
                    .lastLogin(updated.getLastLogin())
                    .build();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil berhasil diperbarui",
                    "data", response));
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
