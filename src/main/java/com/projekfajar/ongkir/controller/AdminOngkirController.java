package com.projekfajar.ongkir.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.ongkir.service.RajaOngkirClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Proxy admin-only ke pencarian lokasi RajaOngkir — dipakai UI pemilih lokasi
 * asal toko di Pengaturan > Pengiriman. Lewat proxy backend supaya kunci API
 * tidak pernah dikirim ke browser. Path ini otomatis admin-only lewat aturan
 * /api/admin/** yang sudah ada di SecurityConfig, tidak perlu aturan baru.
 */
@RestController
@RequestMapping("/api/admin/ongkir")
@RequiredArgsConstructor
@Slf4j
public class AdminOngkirController {

    private final RajaOngkirClient rajaOngkirClient;

    @GetMapping("/cari-lokasi")
    public ResponseEntity<Map<String, Object>> cariLokasi(@RequestParam("q") String query) {
        log.info("Admin location search via RajaOngkir: query={}", query);

        if (!rajaOngkirClient.isConfigured()) {
            log.warn("Location search rejected: RAJAONGKIR_API_KEY is not configured, query={}", query);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "RAJAONGKIR_API_KEY belum diset di server",
                    "data", List.of()));
        }

        List<Map<String, Object>> hasil = rajaOngkirClient.cariLokasi(query);

        log.info("Location search finished: query={}, results={}", query, hasil.size());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", hasil.isEmpty() ? "Tidak ada lokasi yang cocok" : "Lokasi ditemukan",
                "data", hasil));
    }
}
