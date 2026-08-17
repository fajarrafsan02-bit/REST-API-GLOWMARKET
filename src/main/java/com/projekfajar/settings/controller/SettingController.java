package com.projekfajar.settings.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.settings.service.SettingService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SettingController {

    private final SettingService settingService;
    private final SecurityUtils securityUtils;

    @GetMapping("/admin/settings")
    public ResponseEntity<Map<String, Object>> getSettings(Authentication authentication) {
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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pengaturan berhasil diambil",
                    "data", settingService.getMap()));
        } catch (Exception e) {
            log.error("Error getting settings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat pengaturan"));
        }
    }

    @PutMapping("/admin/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody Map<String, String> values,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengubah pengaturan"));
            }

            Map<String, String> updated = settingService.update(values);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pengaturan berhasil diperbarui",
                    "data", updated));
        } catch (Exception e) {
            log.error("Error updating settings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memperbarui pengaturan"));
        }
    }

    @GetMapping("/settings/public")
    public ResponseEntity<Map<String, Object>> getPublicSettings() {
        try {
            Map<String, String> all = settingService.getMap();
            Map<String, Object> store = all.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("store."))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", store));
        } catch (Exception e) {
            log.error("Error getting public settings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat pengaturan"));
        }
    }
}