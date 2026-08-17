package com.projekfajar.wilayah.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.common.ApiResponse;
import com.projekfajar.wilayah.service.WilayahClient;

import lombok.RequiredArgsConstructor;

/**
 * Proxy publik data wilayah Indonesia + kode pos untuk form alamat pembeli.
 * Lewat backend supaya browser pengguna tidak perlu mengakses domain pihak
 * ketiga langsung — form alamat tetap jalan meski DNS/jaringan pengguna
 * memblokir domain itu (lihat WilayahClient).
 */
@RestController
@RequestMapping("/api/wilayah")
@RequiredArgsConstructor
public class WilayahController {

    private final WilayahClient wilayahClient;

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> provinces() {
        return ResponseEntity.ok(ApiResponse.ok(wilayahClient.provinces()));
    }

    @GetMapping("/regencies/{provinceId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> regencies(
            @PathVariable String provinceId) {
        return ResponseEntity.ok(ApiResponse.ok(wilayahClient.regencies(provinceId)));
    }

    @GetMapping("/districts/{regencyId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> districts(
            @PathVariable String regencyId) {
        return ResponseEntity.ok(ApiResponse.ok(wilayahClient.districts(regencyId)));
    }

    @GetMapping("/villages/{districtId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> villages(
            @PathVariable String districtId) {
        return ResponseEntity.ok(ApiResponse.ok(wilayahClient.villages(districtId)));
    }

    @GetMapping("/kode-pos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> kodePos(
            @RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.ok(wilayahClient.cariKodePos(query)));
    }
}
