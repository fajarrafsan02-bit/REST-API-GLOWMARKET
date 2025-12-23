package com.projekfajar.controllers;

import com.projekfajar.DTO.PaymentRequest;
import com.projekfajar.DTO.PaymentResponse;
import com.projekfajar.DTO.XenditInvoiceResponse;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.XenditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final XenditService xenditService;
    private final UserRepository userRepository;

    @PostMapping("/create-invoice")
    public ResponseEntity<Map<String, Object>> createInvoice(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            // If authenticated, set userId
            if (authentication != null) {
                String email = authentication.getName();
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    request.setUserId(user.getId());
                }
            }

            PaymentResponse payment = xenditService.createInvoice(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Invoice berhasil dibuat",
                            "data", payment));
        } catch (Exception e) {
            logger.error("Error creating invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody Map<String, Object> callbackData) {
        try {
            logger.info("Received Xendit webhook: {}", callbackData);

            PaymentResponse payment = xenditService.handleCallback(callbackData);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed successfully",
                    "data", payment));
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()));
        }
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable String externalId) {
        try {
            PaymentResponse payment = xenditService.getPaymentByExternalId(externalId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment details retrieved",
                    "data", payment));
        } catch (Exception e) {
            logger.error("Error getting payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "Payment not found"));
        }
    }

    @GetMapping("/user/history")
    public ResponseEntity<Map<String, Object>> getUserPayments(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            List<PaymentResponse> payments = xenditService.getPaymentsByUser(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment history retrieved",
                    "data", payments));
        } catch (Exception e) {
            logger.error("Error getting user payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()));
        }
    }

    @GetMapping("/check/{invoiceId}")
    public ResponseEntity<Map<String, Object>> checkInvoiceStatus(@PathVariable String invoiceId) {
        try {
            XenditInvoiceResponse invoice = xenditService.checkInvoiceStatus(invoiceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Invoice status checked",
                    "data", invoice));
        } catch (Exception e) {
            logger.error("Error checking invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to check invoice status"));
        }
    }

    @PostMapping("/sync/{externalId}")
    @GetMapping("/sync/{externalId}")
    public ResponseEntity<Map<String, Object>> syncPaymentStatus(@PathVariable String externalId) {
        try {
            // Get payment to extract xenditInvoiceId
            PaymentResponse payment = xenditService.getPaymentByExternalId(externalId);
            
            // Use checkAndUpdateByXenditId which handles status transition properly
            PaymentResponse updated = xenditService.checkAndUpdateByXenditId(payment.getInvoiceId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment status synced",
                    "data", updated));
        } catch (Exception e) {
            logger.error("Error syncing payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to sync: " + e.getMessage()));
        }
    }
    
    @PostMapping("/sync-by-xendit/{xenditInvoiceId}")
    @GetMapping("/sync-by-xendit/{xenditInvoiceId}")
    public ResponseEntity<Map<String, Object>> syncByXenditId(@PathVariable String xenditInvoiceId) {
        try {
            PaymentResponse payment = xenditService.checkAndUpdateByXenditId(xenditInvoiceId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment synced successfully",
                    "data", payment));
        } catch (Exception e) {
            logger.error("Error syncing by Xendit ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to sync: " + e.getMessage()));
        }
    }
}
