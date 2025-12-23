package com.projekfajar.services;

import com.projekfajar.DTO.AlamatResponse;
import com.projekfajar.DTO.PaymentRequest;
import com.projekfajar.DTO.PaymentResponse;
import com.projekfajar.DTO.XenditInvoiceResponse;
import com.projekfajar.models.Alamat;
import com.projekfajar.models.Payment;
import com.projekfajar.models.User;
import com.projekfajar.repository.AlamatRepository;
import com.projekfajar.repository.PaymentRepository;
import com.projekfajar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.projekfajar.services.NotificationService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class XenditService {
    private static final Logger logger = LoggerFactory.getLogger(XenditService.class);

    private final WebClient xenditWebClient;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PesananService pesananService;
    private final AlamatRepository alamatRepository;
    private final NotificationService notificationService;

    @Transactional
    public PaymentResponse createInvoice(PaymentRequest request) {
        logger.info("Creating Xendit invoice for customer: {}", request.getCustomerEmail());

        // Generate unique external ID
        String externalId = "INV-" + UUID.randomUUID().toString();

        // Prepare request body for Xendit
        Map<String, Object> invoiceRequest = new HashMap<>();
        invoiceRequest.put("external_id", externalId);
        invoiceRequest.put("amount", request.getAmount());
        invoiceRequest.put("payer_email", request.getCustomerEmail());
        invoiceRequest.put("description",
                request.getDescription() != null ? request.getDescription() : "Pembayaran Fajar Gold");
        invoiceRequest.put("invoice_duration", 86400); // 24 hours
        invoiceRequest.put("currency", "IDR");
        invoiceRequest.put("reminder_time", 1);

        // Customer details
        Map<String, Object> customer = new HashMap<>();
        customer.put("given_names", request.getCustomerName());
        customer.put("email", request.getCustomerEmail());
        customer.put("mobile_number", request.getCustomerPhone());
        invoiceRequest.put("customer", customer);

        // Call Xendit API
        XenditInvoiceResponse xenditResponse = xenditWebClient.post()
                .uri("/v2/invoices")
                .body(Mono.just(invoiceRequest), Map.class)
                .retrieve()
                .bodyToMono(XenditInvoiceResponse.class)
                .block();

        if (xenditResponse == null) {
            throw new RuntimeException("Failed to create Xendit invoice");
        }

        logger.info("Xendit invoice created successfully: {}", xenditResponse.getId());

        // Get user if userId provided
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        // Get alamat if alamatId provided
        Alamat alamat = null;
        if (request.getAlamatId() != null) {
            alamat = alamatRepository.findById(request.getAlamatId()).orElse(null);
        }

        // Save payment to database
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = ZonedDateTime.parse(xenditResponse.getExpiryDate())
                .toLocalDateTime();

        Payment payment = Payment.builder()
                .externalId(externalId)
                .xenditInvoiceId(xenditResponse.getId())
                .invoiceUrl(xenditResponse.getInvoiceUrl())
                .amount(request.getAmount())
                .status("PENDING")
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .description(request.getDescription())
                .user(user)
                .alamat(alamat)
                .catatan(request.getCatatan())
                .createdAt(now)
                .expiredAt(expiredAt)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        logger.info("Payment saved to database with ID: {}", savedPayment.getId());

        return convertToResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse handleCallback(Map<String, Object> callbackData) {
        logger.info("Processing Xendit callback: {}", callbackData);

        String externalId = (String) callbackData.get("external_id");
        String status = (String) callbackData.get("status");

        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Payment not found with external_id: " + externalId));

        // Capture old status before update
        String oldStatus = payment.getStatus();

        // Update payment status
        payment.setStatus(status.toUpperCase());
        payment.setUpdatedAt(LocalDateTime.now());

        if ("PAID".equalsIgnoreCase(status) || "SETTLED".equalsIgnoreCase(status)) {
            payment.setPaidAt(LocalDateTime.now());
            payment.setStatus("PAID");
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            payment.setStatus("EXPIRED");
        }

        Payment updatedPayment = paymentRepository.save(payment);
        logger.info("Payment status updated to: {}", updatedPayment.getStatus());

        // Only send notification if status changed from non-PAID to PAID
        boolean statusChangedToPaid = !"PAID".equalsIgnoreCase(oldStatus) &&
                "PAID".equalsIgnoreCase(updatedPayment.getStatus());

        if (statusChangedToPaid) {
            try {
                pesananService.createOrderFromPayment(updatedPayment);
                logger.info("Order auto-created for payment: {}", updatedPayment.getId());

                notificationService.sendNewOrderNotification(updatedPayment);
                logger.info("✅ NEW ORDER NOTIFICATION SENT (one time only) for payment: {}", updatedPayment.getId());
            } catch (Exception e) {
                logger.error("Failed to auto-create order: {}", e.getMessage(), e);
            }
        } else {
            logger.info("⏭️ Skipping notification - status unchanged or already PAID (old: {}, new: {})",
                    oldStatus, updatedPayment.getStatus());
        }

        return convertToResponse(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByExternalId(String externalId) {
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return convertToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public XenditInvoiceResponse checkInvoiceStatus(String externalId) {
        logger.info("Checking invoice status for external_id: {}", externalId);

        // Get payment from database first
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Check Xendit API
        XenditInvoiceResponse response = xenditWebClient.get()
                .uri("/v2/invoices/" + payment.getXenditInvoiceId())
                .retrieve()
                .bodyToMono(XenditInvoiceResponse.class)
                .block();

        if (response != null) {
            // Update local database
            payment.setStatus(response.getStatus().toUpperCase());
            payment.setUpdatedAt(LocalDateTime.now());
            if ("PAID".equalsIgnoreCase(response.getStatus()) ||
                    "SETTLED".equalsIgnoreCase(response.getStatus())) {
                payment.setPaidAt(LocalDateTime.now());
                payment.setStatus("PAID");
            } else if ("EXPIRED".equalsIgnoreCase(response.getStatus())) {
                payment.setStatus("EXPIRED");
            }
            paymentRepository.save(payment);
            logger.info("Payment status updated to: {}", payment.getStatus());
        }

        return response;
    }

    @Transactional
    public PaymentResponse checkAndUpdateByXenditId(String xenditInvoiceId) {
        logger.info("Checking invoice status by Xendit ID: {}", xenditInvoiceId);

        // Get payment from database
        Payment payment = paymentRepository.findByXenditInvoiceId(xenditInvoiceId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Capture old status before checking Xendit
        String oldStatus = payment.getStatus();
        logger.info("Current payment status before sync: {}", oldStatus);

        // Check Xendit API
        XenditInvoiceResponse response = xenditWebClient.get()
                .uri("/v2/invoices/" + xenditInvoiceId)
                .retrieve()
                .bodyToMono(XenditInvoiceResponse.class)
                .block();

        if (response != null) {
            // Update status
            payment.setStatus(response.getStatus().toUpperCase());
            payment.setUpdatedAt(LocalDateTime.now());
            if ("PAID".equalsIgnoreCase(response.getStatus()) ||
                    "SETTLED".equalsIgnoreCase(response.getStatus())) {
                payment.setPaidAt(LocalDateTime.now());
                payment.setStatus("PAID");
            } else if ("EXPIRED".equalsIgnoreCase(response.getStatus())) {
                payment.setStatus("EXPIRED");
            }
            payment = paymentRepository.save(payment);
            logger.info("Payment status synced to: {}", payment.getStatus());

            // Only create order and send notification when status changes from non-PAID to
            // PAID
            boolean statusChangedToPaid = !"PAID".equalsIgnoreCase(oldStatus) &&
                    "PAID".equalsIgnoreCase(payment.getStatus());

            if (statusChangedToPaid) {
                logger.info("🆕 Status transition detected: {} -> PAID, processing order...", oldStatus);
                final Payment finalPayment = payment;
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Small delay
                        pesananService.createOrderFromPayment(finalPayment);
                        logger.info("Order auto-created for payment: {}", finalPayment.getId());

                        notificationService.sendNewOrderNotification(finalPayment);
                        logger.info("✅ NEW ORDER NOTIFICATION SENT (one time only) for payment: {}",
                                finalPayment.getId());
                    } catch (Exception e) {
                        logger.error("Failed to auto-create order: {}", e.getMessage(), e);
                    }
                }).start();
            } else {
                logger.info("⏭️ Skipping notification - status unchanged or already PAID (old: {}, new: {})",
                        oldStatus, payment.getStatus());
            }
        }

        return convertToResponse(payment);
    }

    private PaymentResponse convertToResponse(Payment payment) {
        AlamatResponse alamatResponse = null;
        if (payment.getAlamat() != null) {
            Alamat a = payment.getAlamat();
            alamatResponse = AlamatResponse.builder()
                    .id(a.getId())
                    .userId(a.getUser().getId())
                    .namaLengkap(a.getNamaLengkap())
                    .nomorTelepon(a.getNomorTelepon())
                    .alamatLengkap(a.getAlamatLengkap())
                    .provinsi(a.getProvinsi())
                    .kota(a.getKota())
                    .kecamatan(a.getKecamatan())
                    .kelurahan(a.getKelurahan())
                    .kodePos(a.getKodePos())
                    .isDefault(a.getIsDefault())
                    .catatan(a.getCatatan())
                    .createdAt(a.getCreatedAt())
                    .build();
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .externalId(payment.getExternalId())
                .invoiceId(payment.getXenditInvoiceId())
                .invoiceUrl(payment.getInvoiceUrl())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .customerName(payment.getCustomerName())
                .customerEmail(payment.getCustomerEmail())
                .alamat(alamatResponse)
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .build();
    }
}
