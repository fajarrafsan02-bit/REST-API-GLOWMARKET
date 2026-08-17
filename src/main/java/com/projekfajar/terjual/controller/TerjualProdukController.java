package com.projekfajar.terjual.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.terjual.dto.TerjualProdukResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.terjual.service.TerjualProdukService;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/terjual-produk")
@RequiredArgsConstructor
@Slf4j
public class TerjualProdukController {

    private final TerjualProdukService terjualProdukService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTerjualProduk(Authentication authentication) {
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

            List<TerjualProdukResponse> responses = terjualProdukService.getAll();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk terjual berhasil diambil",
                    "data", responses));
        } catch (Exception e) {
            log.error("Error getting terjual produk: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/produk/{produkId}")
    public ResponseEntity<Map<String, Object>> getTerjualByProdukId(
            @PathVariable Long produkId,
            Authentication authentication) {
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

            TerjualProdukResponse response = terjualProdukService.getByProdukId(produkId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data produk terjual berhasil diambil",
                    "data", response));
        } catch (Exception e) {
            log.error("Error getting terjual produk by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
