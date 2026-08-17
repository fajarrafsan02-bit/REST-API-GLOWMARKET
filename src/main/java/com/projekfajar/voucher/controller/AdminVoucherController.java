package com.projekfajar.voucher.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.voucher.dto.VoucherRequest;
import com.projekfajar.voucher.dto.VoucherResponse;
import com.projekfajar.voucher.service.VoucherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kelola voucher diskon. Seluruh endpoint dilindungi /api/admin/** oleh
 * SecurityConfig, jadi hanya admin yang bisa membuat/mengubah voucher.
 */
@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@Slf4j
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        try {
            List<VoucherResponse> data = voucherService.getAll();
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.error("Error listing vouchers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal memuat voucher"));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody VoucherRequest request) {
        try {
            VoucherResponse data = voucherService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "Voucher berhasil dibuat", "data", data));
        } catch (Exception e) {
            log.error("Error creating voucher: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        try {
            VoucherResponse data = voucherService.update(id, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voucher berhasil diperbarui", "data", data));
        } catch (Exception e) {
            log.error("Error updating voucher {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id) {
        try {
            VoucherResponse data = voucherService.toggleAktif(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Status voucher diubah", "data", data));
        } catch (Exception e) {
            log.error("Error toggling voucher {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            voucherService.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voucher dihapus"));
        } catch (Exception e) {
            log.error("Error deleting voucher {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
