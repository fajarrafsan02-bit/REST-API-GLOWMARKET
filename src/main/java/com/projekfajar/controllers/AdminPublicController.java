package com.projekfajar.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.models.Role;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminPublicController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminPublicController.class);
    
    private final UserRepository userRepository;
    
    /**
     * Get list of available admins for customer service chat
     * This is a PUBLIC endpoint - no authentication required
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAdminList() {
        try {
            logger.info("Fetching admin list for customer service");
            
            // Get all users with ADMIN role
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            
            // Map to response DTO (only expose necessary info)
            List<Map<String, Object>> adminList = admins.stream()
                .map(admin -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", admin.getId());
                    map.put("namaLengkap", admin.getNamaLengkap());
                    map.put("email", admin.getEmail());
                    return map;
                })
                .collect(Collectors.toList());
            
            logger.info("Found {} admin(s) available", adminList.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daftar admin berhasil diambil",
                "data", adminList
            ));
            
        } catch (Exception e) {
            logger.error("Error getting admin list: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Gagal memuat daftar admin"
                ));
        }
    }
}
