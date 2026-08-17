package com.projekfajar.tracking.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.tracking.dto.TrackingResponse;
import com.projekfajar.tracking.service.TrackingService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;
    private final PesananRepository pesananRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/{pesananId}")
    public ResponseEntity<Map<String, Object>> getTracking(
            @PathVariable Long pesananId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            Pesanan pesanan = pesananRepository.findById(pesananId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));

            if (!pesanan.getUser().getId().equals(user.getId()) && !securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Anda tidak memiliki akses ke pesanan ini"));
            }

            List<TrackingResponse> timeline = trackingService.getByPesanan(pesananId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Timeline tracking berhasil diambil",
                    "data", timeline));
        } catch (Exception e) {
            log.error("Error mengambil tracking pesanan {}: {}", pesananId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Simulasi/demo: admin memajukan tracking satu tahap. */
    @PostMapping("/{pesananId}/lanjutkan")
    public ResponseEntity<Map<String, Object>> lanjutkan(
            @PathVariable Long pesananId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            if (!securityUtils.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Akses khusus admin"));
            }

            TrackingResponse response = trackingService.lanjutkanStatus(pesananId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tracking dimajukan ke " + response.getStatus(),
                    "data", response));
        } catch (Exception e) {
            log.error("Error melanjutkan tracking pesanan {}: {}", pesananId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
