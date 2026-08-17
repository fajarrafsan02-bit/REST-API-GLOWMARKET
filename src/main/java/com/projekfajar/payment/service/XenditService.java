package com.projekfajar.payment.service;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.UnauthorizedAccessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.projekfajar.pesanan.service.PesananService;
import com.projekfajar.alamat.dto.AlamatResponse;
import com.projekfajar.payment.dto.PaymentRequest;
import com.projekfajar.payment.dto.PaymentResponse;
import com.projekfajar.payment.dto.XenditInvoiceResponse;
import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.keranjang.model.Keranjang;
import com.projekfajar.keranjang.repository.KeranjangRepository;
import com.projekfajar.ongkir.dto.HasilOngkir;
import com.projekfajar.ongkir.service.OngkirCalculationService;
import com.projekfajar.payment.model.Payment;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.user.model.User;
import com.projekfajar.alamat.repository.AlamatRepository;
import com.projekfajar.payment.repository.PaymentRepository;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.voucher.service.VoucherService;
import com.projekfajar.voucher.service.VoucherService.HasilDiskon;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.settings.service.SettingService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class XenditService {

    private final WebClient xenditWebClient;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PesananService pesananService;
    private final AlamatRepository alamatRepository;
    private final NotificationService notificationService;
    private final SettingService settingService;
    private final KeranjangRepository keranjangRepository;
    private final OngkirCalculationService ongkirCalculationService;
    private final VoucherService voucherService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public PaymentResponse createInvoice(PaymentRequest request) {
        log.info("Creating Xendit invoice for customer: {}", request.getCustomerEmail());

        if (request.getUserId() == null) {
            throw new BusinessException("Silakan login terlebih dahulu");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        // Ini satu-satunya jalan membuat invoice, jadi titik yang tepat untuk
        // memastikan bukti pembayaran & status pesanan benar-benar bisa dikirim.
        if (!Boolean.TRUE.equals(user.getEmailTerverifikasi())) {
            throw new BusinessException(
                    "Verifikasi email Anda terlebih dahulu agar bukti pembayaran dan status pesanan bisa kami kirim");
        }

        // Alamat pengiriman wajib dan harus milik user yang bersangkutan
        Alamat alamat = null;
        if (request.getAlamatId() != null) {
            alamat = alamatRepository.findById(request.getAlamatId()).orElse(null);

            if (alamat != null && alamat.getUser() != null
                    && !alamat.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Alamat tidak valid");
            }
        }

        // Nominal dihitung ulang dari data server (keranjang + tarif ongkir),
        // angka dari client tidak dipercaya.
        List<Keranjang> cartItems = keranjangRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("Keranjang kosong");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalBeratGram = 0;
        for (Keranjang item : cartItems) {
            Produk produk = item.getProduk();
            var varian = item.getVariant();

            int stok = varian != null
                    ? (varian.getStock() != null ? varian.getStock() : 0)
                    : (produk.getStock() != null ? produk.getStock() : 0);
            BigDecimal harga = varian != null ? varian.getHarga() : produk.getHarga();

            if (stok < item.getQuantity()) {
                throw new BusinessException("Stok tidak cukup untuk produk: " + produk.getNama());
            }

            subtotal = subtotal.add(harga.multiply(BigDecimal.valueOf(item.getQuantity())));

            if (produk.getBeratGram() != null) {
                totalBeratGram += (int) Math.ceil(produk.getBeratGram() * item.getQuantity());
            }
        }

        // RajaOngkir dicoba dulu (kalau alamat & pengaturan asal sudah lengkap),
        // jatuh ke tarif tetap secara diam-diam kalau gagal dengan cara apa pun.
        // Pembeli cuma boleh mengirim kurir+layanan, bukan harganya — server
        // selalu mengambil ulang tarif sungguhan (lihat OngkirCalculationService).
        HasilOngkir hasilOngkir = ongkirCalculationService.hitung(
                alamat, totalBeratGram, subtotal, request.getKurirCode(), request.getLayanan());
        BigDecimal ongkir = hasilOngkir.getTarif() != null ? hasilOngkir.getTarif() : BigDecimal.ZERO;

        // Diskon dihitung ulang di server terhadap subtotal asli (sebelum ongkir),
        // persis seperti yang ditampilkan di checkout — client tidak bisa memalsukan
        // nominalnya karena hanya mengirim kode.
        BigDecimal diskon = BigDecimal.ZERO;
        if (request.getKodeVoucher() != null && !request.getKodeVoucher().isBlank()) {
            HasilDiskon hasilVoucher = voucherService.hitung(request.getKodeVoucher(), subtotal, user.getId());
            diskon = hasilVoucher.diskon();
        }

        // Xendit menolak nominal berdesimal, jadi dibulatkan ke rupiah penuh.
        BigDecimal amount = subtotal.add(ongkir).subtract(diskon).setScale(0, RoundingMode.HALF_UP);

        if (request.getAmount() != null
                && request.getAmount().subtract(amount).abs().compareTo(BigDecimal.ONE) >= 0) {
            log.warn("Nominal dari client ({}) berbeda dengan hitungan server ({}) — memakai hitungan server",
                    request.getAmount(), amount);
        }

        // Generate unique external ID
        String externalId = "INV-" + UUID.randomUUID().toString();

        // Prepare request body for Xendit
        Map<String, Object> invoiceRequest = new HashMap<>();
        invoiceRequest.put("external_id", externalId);
        invoiceRequest.put("amount", amount);
        invoiceRequest.put("payer_email", request.getCustomerEmail());
        String namaToko = settingService.getValue("store.name");
        if (namaToko == null || namaToko.isBlank()) {
            namaToko = "GlowMarket";
        }
        invoiceRequest.put("description",
                request.getDescription() != null ? request.getDescription() : "Pembayaran " + namaToko);
        invoiceRequest.put("invoice_duration", 86400); // 24 hours
        invoiceRequest.put("currency", "IDR");
        invoiceRequest.put("reminder_time", 1);

        // Setelah pembayaran selesai/gagal di halaman hosted Xendit, pembeli
        // diarahkan otomatis kembali ke halaman status kita — tidak perlu
        // klik manual "kembali ke toko".
        String redirectUrl = frontendUrl + "/payment-status/" + externalId;
        invoiceRequest.put("success_redirect_url", redirectUrl);
        invoiceRequest.put("failure_redirect_url", redirectUrl);

        // Payment methods from settings (empty = all Xendit methods shown).
        // Pembeli boleh mempersempit ke satu metode saja di Checkout — tapi
        // hanya kalau metode itu memang ada dalam daftar yang diaktifkan
        // admin (atau daftar admin kosong = semua metode Xendit aktif),
        // supaya pembeli tidak bisa memaksa metode yang sengaja dimatikan.
        List<String> paymentMethods = settingService.getList("payment.methods");
        String pilihanMetode = request.getPaymentMethod();

        if (pilihanMetode != null && !pilihanMetode.isBlank()
                && (paymentMethods.isEmpty()
                        || paymentMethods.stream().anyMatch(m -> m.equalsIgnoreCase(pilihanMetode)))) {
            invoiceRequest.put("payment_methods", List.of(pilihanMetode.trim().toUpperCase()));
        } else if (!paymentMethods.isEmpty()) {
            invoiceRequest.put("payment_methods", paymentMethods);
        }

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

        log.info("Xendit invoice created successfully: {}", xenditResponse.getId());

        // Save payment to database
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = ZonedDateTime.parse(xenditResponse.getExpiryDate())
                .toLocalDateTime();

        Payment payment = Payment.builder()
                .externalId(externalId)
                .xenditInvoiceId(xenditResponse.getId())
                .invoiceUrl(xenditResponse.getInvoiceUrl())
                .amount(amount)
                .ongkir(ongkir)
                .diskon(diskon)
                .kodeVoucher(request.getKodeVoucher() != null
                        ? request.getKodeVoucher().trim().toUpperCase()
                        : null)
                .ongkirSumber(hasilOngkir.getSumber())
                .ongkirKurir(hasilOngkir.getKurirCode())
                .ongkirLayanan(hasilOngkir.getLayanan())
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
        log.info("Payment saved to database with ID: {}", savedPayment.getId());

        // Kuota voucher baru dipakai setelah invoice benar-benar dibuat,
        // supaya orang yang cuma mengetik kode di checkout tidak menghabiskannya.
        voucherService.pakai(savedPayment.getKodeVoucher());

        // Pesanan langsung dibuat berstatus PENDING agar isinya ter-snapshot
        // dan stoknya direservasi sejak sekarang.
        pesananService.createPendingOrder(savedPayment);

        return convertToResponse(savedPayment);
    }

    /**
     * Satu-satunya tempat status pembayaran berpindah, dipakai oleh webhook
     * maupun sinkronisasi manual, supaya efek sampingnya selalu sama.
     */
    private Payment applyStatusFromXendit(Payment payment, String xenditStatus) {
        String oldStatus = payment.getStatus();
        LocalDateTime now = LocalDateTime.now();

        payment.setUpdatedAt(now);

        if ("PAID".equalsIgnoreCase(xenditStatus) || "SETTLED".equalsIgnoreCase(xenditStatus)) {
            payment.setStatus("PAID");
            payment.setPaidAt(now);
        } else if ("EXPIRED".equalsIgnoreCase(xenditStatus)) {
            payment.setStatus("EXPIRED");
        } else {
            payment.setStatus(xenditStatus.toUpperCase());
        }

        Payment updated = paymentRepository.save(payment);

        boolean becamePaid = !"PAID".equalsIgnoreCase(oldStatus)
                && "PAID".equalsIgnoreCase(updated.getStatus());

        boolean becameUnpayable = !"EXPIRED".equalsIgnoreCase(oldStatus)
                && !"FAILED".equalsIgnoreCase(oldStatus)
                && ("EXPIRED".equalsIgnoreCase(updated.getStatus())
                        || "FAILED".equalsIgnoreCase(updated.getStatus()));

        if (becamePaid) {
            try {
                pesananService.markOrderPaid(updated);
                notificationService.sendNewOrderNotification(updated);
            } catch (Exception e) {
                // Pembayaran sudah masuk tapi pesanan gagal diproses — dulu error ini
                // hanya masuk log dan tidak ada yang tahu. Sekarang admin diberi tahu.
                log.error("Pembayaran {} lunas tetapi pesanan gagal diproses: {}",
                        updated.getExternalId(), e.getMessage(), e);
                notificationService.sendOrderFailureAlert(updated, e.getMessage());
            }
        } else if (becameUnpayable) {
            try {
                pesananService.cancelUnpaidOrder(updated, "pembayaran " + updated.getStatus().toLowerCase());
            } catch (Exception e) {
                log.error("Gagal membatalkan pesanan untuk payment {}: {}",
                        updated.getExternalId(), e.getMessage(), e);
            }
        }

        return updated;
    }

    @Transactional
    public PaymentResponse handleCallback(Map<String, Object> callbackData) {
        log.info("Processing Xendit callback: {}", callbackData);

        String externalId = (String) callbackData.get("external_id");
        String status = (String) callbackData.get("status");

        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("Payment not found with external_id: " + externalId));

        Payment updatedPayment = applyStatusFromXendit(payment, status);
        log.info("Payment status updated to: {}", updatedPayment.getStatus());

        return convertToResponse(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByExternalId(String externalId, User user, boolean admin) {
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("Payment not found"));
        assertOwnerOrAdmin(payment, user, admin);
        return convertToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public XenditInvoiceResponse checkInvoiceStatus(String externalId, User user, boolean admin) {
        log.info("Checking invoice status for external_id: {}", externalId);

        // Get payment from database first
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("Payment not found"));
        assertOwnerOrAdmin(payment, user, admin);

        // Check Xendit API
        XenditInvoiceResponse response = xenditWebClient.get()
                .uri("/v2/invoices/" + payment.getXenditInvoiceId())
                .retrieve()
                .bodyToMono(XenditInvoiceResponse.class)
                .block();

        if (response != null) {
            Payment updated = applyStatusFromXendit(payment, response.getStatus());
            log.info("Payment status updated to: {}", updated.getStatus());
        }

        return response;
    }

    @Transactional
    public PaymentResponse checkAndUpdateByXenditId(String xenditInvoiceId) {
        log.info("Checking invoice status by Xendit ID: {}", xenditInvoiceId);

        // Get payment from database
        Payment payment = paymentRepository.findByXenditInvoiceId(xenditInvoiceId)
                .orElseThrow(() -> new BusinessException("Payment not found"));

        log.info("Current payment status before sync: {}", payment.getStatus());

        // Check Xendit API
        XenditInvoiceResponse response = xenditWebClient.get()
                .uri("/v2/invoices/" + xenditInvoiceId)
                .retrieve()
                .bodyToMono(XenditInvoiceResponse.class)
                .block();

        if (response != null) {
            // Diproses langsung di transaksi ini — sebelumnya dijalankan di Thread
            // terpisah tanpa transaksi, yang bisa berlomba dengan webhook.
            payment = applyStatusFromXendit(payment, response.getStatus());
            log.info("Payment status synced to: {}", payment.getStatus());
        }

        return convertToResponse(payment);
    }

    @Transactional
    public PaymentResponse syncByExternalId(String externalId, User user, boolean admin) {
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("Payment not found"));
        assertOwnerOrAdmin(payment, user, admin);
        return checkAndUpdateByXenditId(payment.getXenditInvoiceId());
    }

    @Transactional
    public PaymentResponse syncByXenditId(String xenditInvoiceId, User user, boolean admin) {
        Payment payment = paymentRepository.findByXenditInvoiceId(xenditInvoiceId)
                .orElseThrow(() -> new BusinessException("Payment not found"));
        assertOwnerOrAdmin(payment, user, admin);
        return checkAndUpdateByXenditId(xenditInvoiceId);
    }

    private void assertOwnerOrAdmin(Payment payment, User user, boolean admin) {
        if (admin) {
            return;
        }
        if (user == null
                || payment.getUser() == null
                || !payment.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Anda tidak memiliki akses ke pembayaran ini");
        }
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
                .ongkir(payment.getOngkir())
                .diskon(payment.getDiskon())
                .kodeVoucher(payment.getKodeVoucher())
                .ongkirSumber(payment.getOngkirSumber())
                .ongkirKurir(payment.getOngkirKurir())
                .ongkirLayanan(payment.getOngkirLayanan())
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
