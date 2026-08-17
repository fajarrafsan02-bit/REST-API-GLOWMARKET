package com.projekfajar.payment.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.exception.UnauthorizedAccessException;
import com.projekfajar.payment.dto.PaymentRequest;
import com.projekfajar.payment.dto.PaymentResponse;
import com.projekfajar.payment.dto.XenditInvoiceResponse;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.user.model.User;
import com.projekfajar.payment.service.XenditService;
import com.projekfajar.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final XenditService xenditService;
    private final SecurityUtils securityUtils;
    private final SettingService settingService;

    @Value("${xendit.callback-token:}")
    private String xenditCallbackToken;

    /**
     * Daftar metode pembayaran aktif untuk ditampilkan sebagai pilihan di
     * Checkout — dibaca-publik karena hanya berisi daftar kode, bukan data
     * sensitif. Kosong = admin belum membatasi, semua metode Xendit aktif
     * boleh ditawarkan (frontend menampilkan daftar bawaan lengkap).
     */
    @GetMapping("/methods")
    public ResponseEntity<Map<String, Object>> getPaymentMethods() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Metode pembayaran aktif",
                "data", settingService.getList("payment.methods")));
    }

    @PostMapping("/create-invoice")
    public ResponseEntity<Map<String, Object>> createInvoice(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            // Invoice selalu dibuat atas nama user yang login — userId dari token,
            // bukan dari body request (agar tidak bisa memesan atas nama orang lain).
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            request.setUserId(user.getId());

            PaymentResponse payment = xenditService.createInvoice(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Invoice berhasil dibuat",
                            "data", payment));
        } catch (Exception e) {
            log.error("Error creating invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody Map<String, Object> callbackData,
            @RequestHeader(value = "x-callback-token", required = false) String callbackToken) {
        try {
            // Tanpa verifikasi ini, siapa pun bisa mengirim status "PAID" palsu
            // dan memicu pembuatan pesanan tanpa pembayaran.
            if (xenditCallbackToken == null || xenditCallbackToken.isBlank()) {
                log.error("xendit.callback-token belum diset — webhook ditolak. "
                        + "Ambil Callback Verification Token di dashboard Xendit lalu set XENDIT_CALLBACK_TOKEN.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Webhook verification is not configured"));
            }

            if (!xenditCallbackToken.equals(callbackToken)) {
                log.warn("Webhook ditolak: x-callback-token tidak cocok");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Invalid callback token"));
            }

            log.info("Received Xendit webhook: {}", callbackData);

            PaymentResponse payment = xenditService.handleCallback(callbackData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed successfully",
                    "data", payment));
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()));
        }
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<Map<String, Object>> getPayment(
            @PathVariable String externalId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            PaymentResponse payment = xenditService.getPaymentByExternalId(
                    externalId, user, securityUtils.isAdmin(user));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment details retrieved",
                    "data", payment));
        } catch (UnauthorizedAccessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "Payment not found"));
        }
    }

    @GetMapping("/user/history")
    public ResponseEntity<Map<String, Object>> getUserPayments(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            List<PaymentResponse> payments = xenditService.getPaymentsByUser(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment history retrieved",
                    "data", payments));
        } catch (Exception e) {
            log.error("Error getting user payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()));
        }
    }

    @GetMapping("/check/{invoiceId}")
    public ResponseEntity<Map<String, Object>> checkInvoiceStatus(
            @PathVariable String invoiceId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            XenditInvoiceResponse invoice = xenditService.checkInvoiceStatus(
                    invoiceId, user, securityUtils.isAdmin(user));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invoice status checked",
                    "data", invoice));
        } catch (UnauthorizedAccessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to check invoice status"));
        }
    }

    @RequestMapping(path = "/sync/{externalId}", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<Map<String, Object>> syncPaymentStatus(
            @PathVariable String externalId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            PaymentResponse updated = xenditService.syncByExternalId(
                    externalId, user, securityUtils.isAdmin(user));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment status synced",
                    "data", updated));
        } catch (UnauthorizedAccessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error syncing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to sync: " + e.getMessage()));
        }
    }

    @RequestMapping(path = "/sync-by-xendit/{xenditInvoiceId}", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<Map<String, Object>> syncByXenditId(
            @PathVariable String xenditInvoiceId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            PaymentResponse payment = xenditService.syncByXenditId(
                    xenditInvoiceId, user, securityUtils.isAdmin(user));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment synced successfully",
                    "data", payment));
        } catch (UnauthorizedAccessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error syncing by Xendit ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to sync: " + e.getMessage()));
        }
    }
}
