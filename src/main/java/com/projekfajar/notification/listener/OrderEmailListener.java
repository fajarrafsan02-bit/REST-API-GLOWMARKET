package com.projekfajar.notification.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.projekfajar.auth.service.EmailService;
import com.projekfajar.notification.event.OrderEmailEvent;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.repository.PesananRepository;

import lombok.RequiredArgsConstructor;

/**
 * Mengirim email pesanan setelah transaksinya benar-benar tersimpan.
 *
 * Dua alasan email tidak dikirim langsung dari service:
 * 1. Bila transaksi gagal setelah email terkirim, pembeli menerima kabar
 *    "pembayaran berhasil" untuk pesanan yang tidak pernah ada.
 * 2. SMTP lambat atau mati tidak boleh menggagalkan pesanan yang sah —
 *    di sini kegagalan hanya dicatat.
 */
@Component
@RequiredArgsConstructor
public class OrderEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEmailListener.class);

    private final PesananRepository pesananRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void kirimEmail(OrderEmailEvent event) {
        try {
            Pesanan pesanan = pesananRepository.findWithItemsById(event.pesananId()).orElse(null);

            if (pesanan == null || pesanan.getUser() == null
                    || pesanan.getUser().getEmail() == null) {
                logger.warn("Lewati email: pesanan {} tidak lengkap", event.pesananId());
                return;
            }

            switch (event.jenis()) {
                case PEMBAYARAN_LUNAS -> emailService.sendPaymentSuccess(pesanan);
                case PERUBAHAN_STATUS -> {
                    // Pembayaran otomatis memindahkan PENDING -> DIKEMAS. Emailnya sudah
                    // diwakili "pembayaran berhasil", jadi tidak dikirim dua kali.
                    if (event.status() == OrderStatus.DIKEMAS && event.dariPending()) {
                        return;
                    }
                    emailService.sendOrderStatus(pesanan, event.status());
                }
            }
        } catch (Exception e) {
            logger.error("Gagal mengirim email untuk pesanan {}: {}",
                    event.pesananId(), e.getMessage(), e);
        }
    }
}
