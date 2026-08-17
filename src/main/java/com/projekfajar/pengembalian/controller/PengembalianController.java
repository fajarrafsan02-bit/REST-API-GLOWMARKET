package com.projekfajar.pengembalian.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.pengembalian.dto.PengembalianRequest;
import com.projekfajar.pengembalian.dto.PengembalianResponse;
import com.projekfajar.pengembalian.service.PengembalianService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/pengembalian")
@RequiredArgsConstructor
@Slf4j
public class PengembalianController {

    private final PengembalianService pengembalianService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<Map<String, Object>> ajukanPengembalian(
            @RequestBody PengembalianRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            PengembalianResponse response = pengembalianService.ajukan(user.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Pengajuan pengembalian berhasil dibuat",
                    "data", response));
        } catch (Exception e) {
            log.error("Error mengajukan pengembalian: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPengembalianUser(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            List<PengembalianResponse> list = pengembalianService.getByUser(user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar pengembalian berhasil diambil",
                    "data", list));
        } catch (Exception e) {
            log.error("Error mengambil pengembalian user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAllPengembalian(
            @RequestParam(required = false) String status,
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

            List<PengembalianResponse> list = pengembalianService.getAll(status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Daftar pengembalian berhasil diambil",
                    "data", list));
        } catch (Exception e) {
            log.error("Error mengambil pengembalian admin: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/setujui")
    public ResponseEntity<Map<String, Object>> setujui(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        return prosesAdmin("setujui", id, body, authentication);
    }

    @PatchMapping("/{id}/tolak")
    public ResponseEntity<Map<String, Object>> tolak(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        return prosesAdmin("tolak", id, body, authentication);
    }

    @PatchMapping("/{id}/terima")
    public ResponseEntity<Map<String, Object>> terima(
            @PathVariable Long id,
            Authentication authentication) {
        return prosesAdmin("terima", id, null, authentication);
    }

    private ResponseEntity<Map<String, Object>> prosesAdmin(
            String aksi, Long id, Map<String, String> body, Authentication authentication) {
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

            String catatan = body != null ? body.get("catatan") : null;
            PengembalianResponse response = switch (aksi) {
                case "setujui" -> pengembalianService.setujui(id, catatan);
                case "tolak" -> pengembalianService.tolak(id, catatan);
                default -> pengembalianService.terima(id);
            };

            String pesan = switch (aksi) {
                case "setujui" -> "Pengembalian disetujui";
                case "tolak" -> "Pengembalian ditolak";
                default -> "Barang pengembalian diterima, stok dipulihkan";
            };

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", pesan,
                    "data", response));
        } catch (Exception e) {
            log.error("Error {} pengembalian {}: {}", aksi, id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
