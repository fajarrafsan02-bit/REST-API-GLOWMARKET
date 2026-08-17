package com.projekfajar.poin.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.poin.dto.PoinResponse;
import com.projekfajar.poin.dto.TukarPoinRequest;
import com.projekfajar.poin.service.PoinLoyalitasService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.SecurityUtils;
import com.projekfajar.voucher.dto.VoucherResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/poin")
@RequiredArgsConstructor
@Slf4j
public class PoinController {

    private final PoinLoyalitasService poinLoyalitasService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/poin rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("GET /api/poin for userId={}", user.getId());
            PoinResponse response = poinLoyalitasService.getStatus(user.getId());
            log.info("Loyalty status returned: userId={} balance={}", user.getId(), response.getSaldoPoin());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data poin berhasil dimuat",
                    "data", response));
        } catch (Exception e) {
            log.error("Failed to load loyalty points: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/tukar")
    public ResponseEntity<Map<String, Object>> tukarVoucher(
            @RequestBody TukarPoinRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("POST /api/poin/tukar rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            log.info("POST /api/poin/tukar userId={} points={}", user.getId(), request.getJumlahPoin());
            VoucherResponse voucher = VoucherResponse.from(
                    poinLoyalitasService.tukarVoucher(user.getId(), request.getJumlahPoin()));
            log.info("Points redeemed: userId={} points={} voucherCode={} value={}",
                    user.getId(), request.getJumlahPoin(), voucher.getKode(), voucher.getNilai());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Poin berhasil ditukar menjadi voucher",
                    "data", Map.of(
                            "kode", voucher.getKode(),
                            "nilai", voucher.getNilai(),
                            "berlakuSampai", voucher.getBerlakuSampai())));
        } catch (Exception e) {
            log.error("Failed to redeem points={}: {}", request.getJumlahPoin(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
