package com.projekfajar.util;

import com.projekfajar.exception.ResourceNotFoundException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.projekfajar.auth.model.Role;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Mengambil user yang sedang login.
     * Mengembalikan {@code null} jika tidak ada autentikasi.
     * Melempar RuntimeException jika token valid tapi user tidak ditemukan di database.
     */
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }
}
