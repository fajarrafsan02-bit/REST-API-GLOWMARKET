package com.projekfajar.notification.service;

import com.projekfajar.exception.UnauthorizedAccessException;

import com.projekfajar.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.projekfajar.notification.dto.NotificationMessage;
import com.projekfajar.notification.mapper.NotificationMapper;
import com.projekfajar.notification.model.Notification;
import com.projekfajar.pengembalian.model.Pengembalian;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.payment.model.Payment;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.user.model.User;
import com.projekfajar.notification.repository.NotificationRepository;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.settings.service.SettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String ADMIN_NOTIFICATION_DESTINATION = "/topic/admin/notifications";

    private static final List<String> ADMIN_NOTIFICATION_TYPES = Arrays.asList(
            "NEW_CUSTOMER",
            "NEW_ORDER",
            "LOW_STOCK",
            "ORDER_FAILED",
            "CHAT_PERLU_BALASAN");

    private static final List<String> USER_NOTIFICATION_TYPES = Arrays.asList(
            "ORDER_SHIPPED",
            "ORDER_COMPLETED",
            "ORDER_REVIEW_REQUEST",
            "RETURN_APPROVED",
            "RETURN_REJECTED",
            "PRODUCT_RESTOCK");

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final ProdukRepository produkRepository;
    private final SettingService settingService;

    private int lowStockThreshold() {
        return settingService.getInt("lowStock.threshold", 5);
    }

    public void sendNewCustomerNotification(User user) {
        try {
            Notification entity = Notification.builder()
                    .type("NEW_CUSTOMER")
                    .title("Pelanggan baru terdaftar")
                    .message("Customer baru: " + user.getNamaLengkap() + " (" + user.getEmail() + ")")
                    .userId(null)
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, NotificationMapper.toMessage(entity));
            log.info("New customer notification sent for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send new customer notification: {}", e.getMessage(), e);
        }
    }

    public void sendNewOrderNotification(Payment payment) {
        try {
            Notification entity = Notification.builder()
                    .type("NEW_ORDER")
                    .title("Pesanan baru masuk")
                    .message("Pesanan baru dari " + payment.getCustomerName() +
                            ", total: " + payment.getAmount())
                    .userId(null)
                    .paymentId(payment.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, NotificationMapper.toMessage(entity));
            log.info("New order notification sent for payment: {}", payment.getId());
        } catch (Exception e) {
            log.error("Failed to send new order notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Pembayaran sudah lunas tetapi pesanannya gagal diproses.
     * Butuh tindakan manual admin, jadi harus terlihat — bukan sekadar masuk log.
     */
    public void sendOrderFailureAlert(Payment payment, String alasan) {
        try {
            Notification entity = Notification.builder()
                    .type("ORDER_FAILED")
                    .title("Pembayaran lunas tapi pesanan gagal dibuat")
                    .message("Invoice " + payment.getExternalId() + " atas nama " + payment.getCustomerName()
                            + " sudah dibayar (Rp " + payment.getAmount() + ") tetapi pesanannya gagal diproses: "
                            + alasan + ". Perlu ditindaklanjuti manual.")
                    .paymentId(payment.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, NotificationMapper.toMessage(entity));
            log.error("ORDER_FAILED notification sent for payment: {}", payment.getExternalId());
        } catch (Exception e) {
            log.error("Failed to send order failure alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Pelanggan bertanya sesuatu yang tidak bisa dijawab bot saat admin offline.
     * Tanpa ini, pertanyaan di luar jangkauan bot justru lebih mudah terlewat
     * daripada sebelum ada bot.
     */
    public void sendPertanyaanBelumTerjawab(User pelanggan, String pesan) {
        try {
            String ringkas = pesan != null && pesan.length() > 120
                    ? pesan.substring(0, 120) + "..."
                    : pesan;

            Notification entity = Notification.builder()
                    .type("CHAT_PERLU_BALASAN")
                    .title("Pertanyaan pelanggan menunggu balasan")
                    .message(pelanggan.getNamaLengkap() + " bertanya: \"" + ringkas
                            + "\" — bot belum bisa menjawabnya.")
                    .userId(null)
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION,
                    NotificationMapper.toMessage(entity));
            log.info("Notifikasi pertanyaan belum terjawab dikirim untuk user {}", pelanggan.getId());
        } catch (Exception e) {
            log.error("Failed to send unanswered question notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Mengirim notifikasi stok menipis untuk satu produk.
     * Cek duplikasi dalam 24 jam terakhir untuk mencegah spam.
     */
    public void sendLowStockNotification(Produk produk) {
        try {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<Notification> recentNotifications = notificationRepository
                    .findByProdukIdAndTypeAndCreatedAtAfter(produk.getId(), "LOW_STOCK", oneDayAgo);

            if (!recentNotifications.isEmpty()) {
                log.info("Low stock notification already sent for product {} in last 24 hours", produk.getId());
                return;
            }

            Notification entity = Notification.builder()
                    .type("LOW_STOCK")
                    .title("Stok Produk Kritis!")
                    .message(String.format("Produk '%s' tersisa %d unit. Segera lakukan restok!",
                            produk.getNama(), produk.getStock()))
                    .produkId(produk.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, NotificationMapper.toMessage(entity));
            log.info("Low stock notification sent for product: {} (stock: {})",
                    produk.getNama(), produk.getStock());
        } catch (Exception e) {
            log.error("Failed to send low stock notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Cek semua produk dan kirim notifikasi untuk stok di bawah ambang.
     */
    public void checkAndNotifyLowStockProducts(List<Produk> allProducts) {
        try {
            int threshold = lowStockThreshold();
            int count = 0;
            for (Produk produk : allProducts) {
                if (produk.getStock() != null && produk.getStock() < threshold) {
                    sendLowStockNotification(produk);
                    count++;
                }
            }
            log.info("Checked {} products, sent {} low stock notifications", allProducts.size(), count);
        } catch (Exception e) {
            log.error("Failed to check low stock products: {}", e.getMessage(), e);
        }
    }

    public long checkLowStockProducts() {
        int threshold = lowStockThreshold();
        List<Produk> allProducts = produkRepository.findByDeletedFalse();
        checkAndNotifyLowStockProducts(allProducts);
        long lowStockCount = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() < threshold)
                .count();
        log.info("Low stock check completed, found {} products", lowStockCount);
        return lowStockCount;
    }

    /**
     * Kabar hasil pengajuan pengembalian ke pembeli: disetujui (uang
     * dikembalikan) atau ditolak (dengan catatan admin bila ada).
     */
    public void sendPengembalianNotification(Pengembalian pengembalian, boolean disetujui) {
        try {
            String type = disetujui ? "RETURN_APPROVED" : "RETURN_REJECTED";
            String title = disetujui ? "Pengembalian Disetujui" : "Pengembalian Ditolak";
            String message;
            if (disetujui) {
                message = "Pengajuan " + pengembalian.getNomorPengembalian() + " disetujui. "
                        + "Refund sebesar Rp " + pengembalian.getJumlahRefund() + " akan dikembalikan.";
            } else {
                message = "Pengajuan " + pengembalian.getNomorPengembalian() + " ditolak."
                        + (pengembalian.getCatatanAdmin() != null
                                ? " Alasan: " + pengembalian.getCatatanAdmin()
                                : " Hubungi admin untuk keterangan lebih lanjut.");
            }

            Notification entity = Notification.builder()
                    .type(type)
                    .title(title)
                    .message(message)
                    .userId(pengembalian.getUser().getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            String userDestination = "/topic/notifications/user/" + pengembalian.getUser().getId();
            messagingTemplate.convertAndSend(userDestination, NotificationMapper.toMessage(entity));
            log.info("Pengembalian notification sent to user {}: {}",
                    pengembalian.getUser().getId(), type);
        } catch (Exception e) {
            log.error("Failed to send pengembalian notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Kabar stok produk kembali tersedia bagi pembeli yang mendaftar notifikasi.
     */
    public void sendRestockNotification(User user, String namaProduk, String namaVariant, Long produkId) {
        try {
            String nama = namaVariant != null && !namaVariant.isBlank()
                    ? namaProduk + " (" + namaVariant + ")"
                    : namaProduk;

            Notification entity = Notification.builder()
                    .type("PRODUCT_RESTOCK")
                    .title("Produk Kembali Tersedia!")
                    .message("Produk '" + nama + "' sudah kembali tersedia. Jangan sampai kehabisan ya!")
                    .userId(user.getId())
                    .produkId(produkId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            String userDestination = "/topic/notifications/user/" + user.getId();
            messagingTemplate.convertAndSend(userDestination, NotificationMapper.toMessage(entity));
            log.info("Restock notification sent to user {} for product {}",
                    user.getId(), produkId);
        } catch (Exception e) {
            log.error("Failed to send restock notification: {}", e.getMessage(), e);
        }
    }

    public void sendOrderStatusNotification(Pesanan pesanan, OrderStatus newStatus) {
        try {
            if (newStatus != OrderStatus.DIKIRIM && newStatus != OrderStatus.SELESAI) {
                log.debug("Skipping notification for status: {}", newStatus);
                return;
            }

            String type;
            String title;
            String message;

            if (newStatus == OrderStatus.DIKIRIM) {
                type = "ORDER_SHIPPED";
                title = "Pesanan Sedang Dikirim";
                message = String.format("Pesanan #%s sedang dalam perjalanan. Nomor Resi: %s",
                        pesanan.getNomorPesanan(),
                        pesanan.getNomorResi() != null ? pesanan.getNomorResi() : "-");
            } else {
                type = "ORDER_REVIEW_REQUEST";
                title = "Beri Rating & Ulasan";
                message = String.format("Pesanan #%s telah selesai. Bagaimana kualitas produknya? "
                        + "Beri rating dan ulasan Anda ya!", pesanan.getNomorPesanan());
            }

            Notification entity = Notification.builder()
                    .type(type)
                    .title(title)
                    .message(message)
                    .userId(pesanan.getUser().getId())
                    .paymentId(pesanan.getPayment() != null ? pesanan.getPayment().getId() : null)
                    .createdAt(LocalDateTime.now())
                    .build();
            entity = notificationRepository.save(entity);

            String userDestination = "/topic/notifications/user/" + pesanan.getUser().getId();
            messagingTemplate.convertAndSend(userDestination, NotificationMapper.toMessage(entity));

            log.info("Order status notification sent to user {} for order {}: {}",
                    pesanan.getUser().getId(), pesanan.getNomorPesanan(), newStatus);
        } catch (Exception e) {
            log.error("Failed to send order status notification: {}", e.getMessage(), e);
        }
    }

    public List<NotificationMessage> getAdminNotifications() {
        List<Notification> entities = notificationRepository
                .findTop50ByTypeInOrderByCreatedAtDesc(ADMIN_NOTIFICATION_TYPES);
        return entities.stream().map(NotificationMapper::toMessage).toList();
    }

    public Long getAdminUnreadCount() {
        return notificationRepository.countByTypeInAndReadFalse(ADMIN_NOTIFICATION_TYPES);
    }

    public int markAllAdminRead() {
        List<Notification> unreadNotifications = notificationRepository.findByTypeInAndReadFalse(ADMIN_NOTIFICATION_TYPES);
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read", unreadNotifications.size());
        return unreadNotifications.size();
    }

    public Map<String, Object> markAdminNotificationRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notifikasi tidak ditemukan"));

        boolean alreadyRead = Boolean.TRUE.equals(notification.getRead());
        if (!alreadyRead) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification {} marked as read", id);
        }

        return Map.of(
                "alreadyRead", alreadyRead,
                "data", NotificationMapper.toMessage(notification));
    }

    public List<NotificationMessage> getUserNotifications(Long userId) {
        List<Notification> entities = notificationRepository
                .findByTypeInAndUserIdOrderByCreatedAtDesc(USER_NOTIFICATION_TYPES, userId);
        return entities.stream().map(NotificationMapper::toMessage).toList();
    }

    public Long getUserUnreadCount(Long userId) {
        return notificationRepository.countByTypeInAndUserIdAndReadFalse(USER_NOTIFICATION_TYPES, userId);
    }

    public int markAllUserRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByTypeInAndUserIdAndReadFalse(USER_NOTIFICATION_TYPES, userId);
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(n -> {
            n.setRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
        return unreadNotifications.size();
    }

    public Map<String, Object> markUserNotificationRead(Long userId, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notifikasi tidak ditemukan"));

        if (notification.getUserId() == null || !notification.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Tidak memiliki akses ke notifikasi ini");
        }

        boolean alreadyRead = Boolean.TRUE.equals(notification.getRead());
        if (!alreadyRead) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("User notification {} marked as read by user {}", id, userId);
        }

        return Map.of(
                "alreadyRead", alreadyRead,
                "data", NotificationMapper.toMessage(notification));
    }
}
