package com.projekfajar.services;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.projekfajar.DTO.NotificationMessage;
import com.projekfajar.models.Notification;
import com.projekfajar.models.OrderStatus;
import com.projekfajar.models.Payment;
import com.projekfajar.models.Pesanan;
import com.projekfajar.models.Produk;
import com.projekfajar.models.User;
import com.projekfajar.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String ADMIN_NOTIFICATION_DESTINATION = "/topic/admin/notifications";

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public void sendNewCustomerNotification(User user) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Save to database (userId NULL = for admin)
            Notification entity = Notification.builder()
                    .type("NEW_CUSTOMER")
                    .title("Pelanggan baru terdaftar")
                    .message("Customer baru: " + user.getNamaLengkap() + " (" + user.getEmail() + ")")
                    .userId(null) // NULL = admin notification
                    .createdAt(now)
                    .build();
            entity = notificationRepository.save(entity);

            // Build DTO for WebSocket
            NotificationMessage notification = NotificationMessage.builder()
                    .id(entity.getId())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .message(entity.getMessage())
                    .userId(entity.getUserId())
                    .paymentId(entity.getPaymentId())
                    .timestamp(entity.getCreatedAt())
                    .build();

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, notification);
            logger.info("New customer notification sent for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send new customer notification: {}", e.getMessage(), e);
        }
    }

    public void sendNewOrderNotification(Payment payment) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Save to database (userId NULL = for admin)
            Notification entity = Notification.builder()
                    .type("NEW_ORDER")
                    .title("Pesanan baru masuk")
                    .message("Pesanan baru dari " + payment.getCustomerName() +
                            ", total: " + payment.getAmount())
                    .userId(null) // NULL = admin notification
                    .paymentId(payment.getId())
                    .createdAt(now)
                    .build();
            entity = notificationRepository.save(entity);

            // Build DTO for WebSocket
            NotificationMessage notification = NotificationMessage.builder()
                    .id(entity.getId())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .message(entity.getMessage())
                    .userId(entity.getUserId())
                    .paymentId(entity.getPaymentId())
                    .timestamp(entity.getCreatedAt())
                    .build();

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, notification);
            logger.info("New order notification sent for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to send new order notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send low stock notification for a single product
     * This will check if notification already exists in last 24 hours to avoid spam
     */
    public void sendLowStockNotification(Produk produk) {
        try {
            // Check if already sent notification for this product in last 24 hours
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<Notification> recentNotifications = notificationRepository
                    .findByProdukIdAndTypeAndCreatedAtAfter(produk.getId(), "LOW_STOCK", oneDayAgo);
            
            if (!recentNotifications.isEmpty()) {
                logger.info("Low stock notification already sent for product {} in last 24 hours", produk.getId());
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            // Save to database
            Notification entity = Notification.builder()
                    .type("LOW_STOCK")
                    .title("Stok Produk Kritis!")
                    .message(String.format("Produk '%s' tersisa %d unit. Segera lakukan restok!", 
                            produk.getNama(), produk.getStock()))
                    .produkId(produk.getId())
                    .createdAt(now)
                    .build();
            entity = notificationRepository.save(entity);

            // Build DTO for WebSocket
            NotificationMessage notification = NotificationMessage.builder()
                    .id(entity.getId())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .message(entity.getMessage())
                    .produkId(entity.getProdukId())
                    .timestamp(entity.getCreatedAt())
                    .build();

            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_DESTINATION, notification);
            logger.info("Low stock notification sent for product: {} (stock: {})", 
                    produk.getNama(), produk.getStock());
        } catch (Exception e) {
            logger.error("Failed to send low stock notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Check all products and send notifications for low stock items (< 5)
     */
    public void checkAndNotifyLowStockProducts(List<Produk> allProducts) {
        try {
            int count = 0;
            for (Produk produk : allProducts) {
                if (produk.getStock() != null && produk.getStock() < 5) {
                    sendLowStockNotification(produk);
                    count++;
                }
            }
            logger.info("Checked {} products, sent {} low stock notifications", allProducts.size(), count);
        } catch (Exception e) {
            logger.error("Failed to check low stock products: {}", e.getMessage(), e);
        }
    }

    /**
     * Send order status update notification to user
     * Triggered when order status changes to DIKIRIM or SELESAI
     */
    public void sendOrderStatusNotification(Pesanan pesanan, OrderStatus newStatus) {
        try {
            if (newStatus != OrderStatus.DIKIRIM && newStatus != OrderStatus.SELESAI) {
                logger.debug("Skipping notification for status: {}", newStatus);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            String title;
            String message;
            String type;

            if (newStatus == OrderStatus.DIKIRIM) {
                type = "ORDER_SHIPPED";
                title = "Pesanan Sedang Dikirim";
                message = String.format("Pesanan #%s sedang dalam perjalanan. Nomor Resi: %s",
                        pesanan.getNomorPesanan(),
                        pesanan.getNomorResi() != null ? pesanan.getNomorResi() : "-");
            } else {
                type = "ORDER_COMPLETED";
                title = "Pesanan Selesai";
                message = String.format("Pesanan #%s telah selesai. Terima kasih atas pembelian Anda!",
                        pesanan.getNomorPesanan());
            }

            // Save to database
            Notification entity = Notification.builder()
                    .type(type)
                    .title(title)
                    .message(message)
                    .userId(pesanan.getUser().getId())
                    .paymentId(pesanan.getPayment() != null ? pesanan.getPayment().getId() : null)
                    .createdAt(now)
                    .build();
            entity = notificationRepository.save(entity);

            // Build DTO for WebSocket
            NotificationMessage notification = NotificationMessage.builder()
                    .id(entity.getId())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .message(entity.getMessage())
                    .userId(entity.getUserId())
                    .paymentId(entity.getPaymentId())
                    .timestamp(entity.getCreatedAt())
                    .build();

            // Send to specific user via WebSocket
            String userDestination = "/topic/user/" + pesanan.getUser().getId() + "/notifications";
            messagingTemplate.convertAndSend(userDestination, notification);
            
            logger.info("Order status notification sent to user {} for order {}: {}",
                    pesanan.getUser().getId(), pesanan.getNomorPesanan(), newStatus);
        } catch (Exception e) {
            logger.error("Failed to send order status notification: {}", e.getMessage(), e);
        }
    }
}
