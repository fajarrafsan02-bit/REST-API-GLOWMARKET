package com.projekfajar.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.user.dto.CustomerResponse;
import com.projekfajar.user.dto.UpdateProfilRequest;
import com.projekfajar.user.dto.UserResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.user.service.UserService;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            UserResponse profile = userService.getProfile(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil berhasil diambil",
                    "data", profile));
        } catch (Exception e) {
            log.error("Error getting profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/profile/admin")
    public ResponseEntity<Map<String, Object>> getAdminProfile(Authentication authentication) {
        try {
            User admin = securityUtils.getCurrentUser(authentication);
            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(admin)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses"));
            }

            UserResponse profile = userService.getAdminProfile(admin);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil admin berhasil diambil",
                    "data", profile));
        } catch (Exception e) {
            log.error("Error getting admin profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/total-pelanggan")
    public ResponseEntity<Map<String, Object>> getTotalPelanggan(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            long totalPelanggan = userService.getTotalPelanggan();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Total pelanggan berhasil diambil",
                    "data", totalPelanggan));
        } catch (Exception e) {
            log.error("Error getting total pelanggan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getAllCustomers(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses data ini"));
            }

            List<CustomerResponse> customers = userService.getAllCustomers();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data pelanggan berhasil diambil",
                    "data", customers));
        } catch (Exception e) {
            log.error("Error getting all customers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat data: " + e.getMessage()));
        }
    }

    @PutMapping("/customers/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleCustomerStatus(
            @org.springframework.web.bind.annotation.PathVariable("id") Long id,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah status pelanggan"));
            }

            CustomerResponse response = userService.toggleCustomerStatus(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status pengguna berhasil diperbarui",
                    "data", response));
        } catch (Exception e) {
            log.error("Error toggling customer status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody UpdateProfilRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            UserResponse response = userService.updateProfile(user, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profil berhasil diperbarui",
                    "data", response));
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
