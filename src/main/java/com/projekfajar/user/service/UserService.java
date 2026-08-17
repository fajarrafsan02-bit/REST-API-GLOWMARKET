package com.projekfajar.user.service;

import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.exception.BusinessException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.user.dto.CustomerResponse;
import com.projekfajar.user.dto.UpdateProfilRequest;
import com.projekfajar.user.dto.UserResponse;
import com.projekfajar.auth.model.Role;
import com.projekfajar.user.model.User;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PesananRepository pesananRepository;

    public UserResponse getProfile(User user) {
        log.info("Fetching profile for user {} (ID: {})", user.getEmail(), user.getId());
        return toUserResponse(user);
    }

    public UserResponse getAdminProfile(User admin) {
        log.info("Fetching admin profile for {} (ID: {})", admin.getEmail(), admin.getId());
        return toUserResponse(admin);
    }

    public long getTotalPelanggan() {
        long total = userRepository.countByRole(Role.USER);
        log.info("Counted total customers with role USER: {}", total);
        return total;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        List<CustomerResponse> customers = userRepository.findByRole(Role.USER).stream()
                .map(this::toCustomerResponse)
                .toList();
        log.info("Fetched customer list for admin view, total: {}", customers.size());
        return customers;
    }

    @Transactional
    public CustomerResponse toggleCustomerStatus(Long userId) {
        log.info("Admin toggling status for customer userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Toggle status failed: user ID {} not found", userId);
                    return new ResourceNotFoundException("Pengguna tidak ditemukan");
                });

        if (user.getRole() == Role.ADMIN) {
            log.warn("Rejected attempt to toggle status of ADMIN account userId: {}", userId);
            throw new BusinessException("Tidak dapat mengubah status akun Admin");
        }

        boolean currentStatus = !Boolean.FALSE.equals(user.getTerverifikasi());
        user.setTerverifikasi(!currentStatus);
        User updated = userRepository.save(user);

        log.info("User ID {} status successfully toggled to terverifikasi={}", userId, updated.getTerverifikasi());
        return toCustomerResponse(updated);
    }

    public UserResponse updateProfile(User user, UpdateProfilRequest request) {
        log.info("Updating profile for user ID: {} ({})", user.getId(), user.getEmail());

        if (request.getNamaLengkap() != null && !request.getNamaLengkap().isEmpty()) {
            user.setNamaLengkap(request.getNamaLengkap());
        }

        if (request.getNoHp() != null && !request.getNoHp().isEmpty()) {
            user.setNoHp(request.getNoHp());
        }

        if (request.getPasswordBaru() != null && !request.getPasswordBaru().isEmpty()) {
            log.info("User ID: {} requested a password change", user.getId());
            if (request.getPasswordLama() == null || request.getPasswordLama().isEmpty()) {
                log.warn("Password change failed for user ID {}: old password is missing", user.getId());
                throw new BusinessException("Password lama harus diisi");
            }

            if (!passwordEncoder.matches(request.getPasswordLama(), user.getPassword())) {
                log.warn("Password change failed for user ID {}: old password mismatch", user.getId());
                throw new BusinessException("Password lama tidak sesuai");
            }

            if (request.getPasswordBaru().length() < 6) {
                log.warn("Password change failed for user ID {}: new password too short", user.getId());
                throw new BusinessException("Password baru minimal 6 karakter");
            }

            user.setPassword(passwordEncoder.encode(request.getPasswordBaru()));
            log.info("Password for user ID: {} successfully updated", user.getId());
        }

        User updated = userRepository.save(user);
        log.info("Profile for user ID: {} successfully updated", updated.getId());
        return toUserResponse(updated);
    }

    public List<Map<String, Object>> getAdminList() {
        log.info("Fetching admin list");
        return userRepository.findByRole(Role.ADMIN).stream()
                .map(admin -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", admin.getId());
                    map.put("namaLengkap", admin.getNamaLengkap());
                    map.put("email", admin.getEmail());
                    return map;
                })
                .toList();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .namaLengkap(user.getNamaLengkap())
                .email(user.getEmail())
                .noHp(user.getNoHp())
                .role(user.getRole())
                .terverifikasi(user.getTerverifikasi())
                .emailTerverifikasi(user.getEmailTerverifikasi())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    private CustomerResponse toCustomerResponse(User customer) {
        try {
            Long totalOrders = pesananRepository.countByUserId(customer.getId());
            if (totalOrders == null) {
                totalOrders = 0L;
            }

            BigDecimal totalSpent = BigDecimal.ZERO;
            try {
                totalSpent = pesananRepository.findByUserId(customer.getId()).stream()
                        .map(pesanan -> pesanan.getTotalHarga() != null ? pesanan.getTotalHarga() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception e) {
                log.warn("Failed to calculate total spent for user {}: {}", customer.getId(), e.getMessage());
            }

            return CustomerResponse.builder()
                    .id(customer.getId())
                    .nama(customer.getNamaLengkap())
                    .email(customer.getEmail())
                    .phone(customer.getNoHp())
                    .role(customer.getRole())
                    .isActive(customer.getTerverifikasi() != null ? customer.getTerverifikasi() : false)
                    .createdAt(customer.getCreatedAt())
                    .lastLogin(customer.getLastLogin())
                    .totalOrders(totalOrders)
                    .totalSpent(totalSpent)
                    .build();
        } catch (Exception e) {
            log.error("Failed to process customer data {}: {}", customer.getId(), e.getMessage(), e);
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
                    .totalSpent(BigDecimal.ZERO)
                    .build();
        }
    }
}
