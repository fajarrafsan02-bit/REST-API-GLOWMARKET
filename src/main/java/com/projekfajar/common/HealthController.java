package com.projekfajar.common;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Titik periksa kesehatan untuk platform hosting (Render, Railway, dsb.).
 *
 * Platform memanggil endpoint ini berkala untuk memastikan proses masih
 * hidup; instance yang tidak membalas akan dianggap mati dan di-restart.
 * Sengaja tanpa akses database supaya gangguan database tidak ikut membuat
 * instance dinyatakan mati dan direstart terus-menerus.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("success", true, "status", "UP"));
    }
}
