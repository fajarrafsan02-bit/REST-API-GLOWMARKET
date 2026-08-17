package com.projekfajar.payment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.projekfajar.payment.model.Payment;
import com.projekfajar.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Menutup invoice yang sudah lewat masa berlaku.
 *
 * Tanpa ini, pembayaran yang tidak pernah diselesaikan akan berstatus PENDING
 * selamanya dan stok yang direservasi saat checkout tidak pernah kembali —
 * terutama bila webhook Xendit tidak sampai ke server.
 *
 * Status tidak pernah ditebak: setiap invoice dikonfirmasi dulu ke Xendit.
 * Jadi invoice yang ternyata sudah dibayar (webhook-nya hilang) justru
 * ikut terselamatkan di sini, bukan salah dibatalkan.
 */
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentRepository paymentRepository;
    private final XenditService xenditService;

    @Scheduled(fixedDelayString = "${payment.reconciliation-interval-ms:600000}")
    public void reconcileOverduePayments() {
        List<Payment> overdue = paymentRepository.findByStatusAndExpiredAtBefore(
                "PENDING", LocalDateTime.now());

        if (overdue.isEmpty()) {
            return;
        }

        logger.info("Merekonsiliasi {} invoice yang sudah lewat masa berlaku", overdue.size());

        for (Payment payment : overdue) {
            try {
                // Menanyakan status sebenarnya ke Xendit; efek sampingnya
                // (buat pesanan / batalkan + kembalikan stok) ditangani di sana.
                xenditService.checkAndUpdateByXenditId(payment.getXenditInvoiceId());
            } catch (Exception e) {
                // Biarkan tetap PENDING dan coba lagi pada siklus berikutnya —
                // lebih aman daripada menebak statusnya.
                logger.error("Gagal merekonsiliasi invoice {}: {}",
                        payment.getExternalId(), e.getMessage());
            }
        }
    }
}
