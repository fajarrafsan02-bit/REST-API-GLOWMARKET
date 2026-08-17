package com.projekfajar.voucher.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;
import com.projekfajar.voucher.dto.VoucherResponse;
import com.projekfajar.voucher.service.VoucherService;
import com.projekfajar.voucher.service.VoucherService.HasilDiskon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint untuk mengecek voucher di halaman checkout.
 * Hanya mengembalikan hasil hitungan diskon — pemakaian kuota dicatat
 * belakangan saat invoice benar-benar dibuat (lihat XenditService).
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {

    private final VoucherService voucherService;
    private final SecurityUtils securityUtils;

    /** Voucher umum yang sedang aktif — ditampilkan di halaman Poin Loyalitas. */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublic() {
        List<VoucherResponse> data = voucherService.getPublicAktif();
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String kode = body.get("kode");
        BigDecimal subtotal = body.get("subtotal") != null && !body.get("subtotal").isBlank()
                ? new BigDecimal(body.get("subtotal"))
                : BigDecimal.ZERO;

        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            HasilDiskon hasil = voucherService.hitung(kode, subtotal, user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", hasil.pesan(),
                    "data", Map.of(
                            "kode", hasil.voucher().getKode(),
                            "jenis", hasil.voucher().getJenis(),
                            "nilai", hasil.voucher().getNilai(),
                            "diskon", hasil.diskon(),
                            "minBelanja", hasil.voucher().getMinBelanja())));
        } catch (Exception e) {
            log.debug("Cek voucher {} gagal: {}", kode, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
