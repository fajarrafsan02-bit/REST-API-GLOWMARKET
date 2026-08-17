package com.projekfajar.user.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.auth.model.Role;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminPublicController {
    
    
    private final UserRepository userRepository;
    
    /**
     * Daftar admin untuk memulai chat customer service.
     *
     * Wajib login, dan hanya mengembalikan id + nama. Sebelumnya endpoint ini
     * terbuka untuk siapa pun dan ikut membocorkan alamat email admin.
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAdminList() {
        try {
            log.info("Fetching admin list for customer service");
            
            // Get all users with ADMIN role
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            
            // Map to response DTO (only expose necessary info)
            List<Map<String, Object>> adminList = admins.stream()
                .map(admin -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", admin.getId());
                    map.put("namaLengkap", admin.getNamaLengkap());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("Found {} admin(s) available", adminList.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daftar admin berhasil diambil",
                "data", adminList
            ));
            
        } catch (Exception e) {
            log.error("Error getting admin list: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Gagal memuat daftar admin"
                ));
        }
    }
}
