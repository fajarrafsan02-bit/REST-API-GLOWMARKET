package com.projekfajar.services;

import com.projekfajar.DTO.AlamatResponse;
import com.projekfajar.DTO.PesananItemResponse;
import com.projekfajar.DTO.PesananResponse;
import com.projekfajar.DTO.PaymentResponse;
import com.projekfajar.models.*;
import com.projekfajar.repository.KeranjangRepository;
import com.projekfajar.repository.PesananRepository;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.repository.ProdukTerjualRepository;
import com.projekfajar.repository.TerjualProdukRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PesananService {
    private static final Logger logger = LoggerFactory.getLogger(PesananService.class);
    
    private final PesananRepository pesananRepository;
    private final KeranjangRepository keranjangRepository;
    private final UserRepository userRepository;
    private final com.projekfajar.repository.ProdukRepository produkRepository;
    private final ProdukTerjualRepository produkTerjualRepository;
    private final TerjualProdukRepository terjualProdukRepository;
    private final NotificationService notificationService;
    
    @Transactional
    public PesananResponse createOrderFromPayment(Payment payment) {
        logger.info("Creating order from payment: {}", payment.getId());
        
        // Check if order already exists
        if (pesananRepository.existsByPaymentId(payment.getId())) {
            logger.warn("Order already exists for payment: {}", payment.getId());
            return convertToResponse(pesananRepository.findByPaymentId(payment.getId()).orElseThrow());
        }
        
        User user = payment.getUser();
        if (user == null) {
            throw new RuntimeException("Payment tidak terkait dengan user");
        }
        
        // Get cart items
        List<Keranjang> cartItems = keranjangRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Keranjang kosong, tidak bisa membuat pesanan");
        }
        
        // Generate order number
        String nomorPesanan = generateOrderNumber();
        
        // Create order
        Pesanan pesanan = Pesanan.builder()
                .nomorPesanan(nomorPesanan)
                .user(user)
                .payment(payment)
                .totalHarga(payment.getAmount())
                .status(OrderStatus.DIKEMAS) // Auto set to DIKEMAS when paid
                .alamat(payment.getAlamat())
                .catatan(payment.getCatatan())
                .createdAt(LocalDateTime.now())
                .dikemasAt(LocalDateTime.now())
                .build();
        
        // Create order items from cart and reduce stock
        LocalDateTime tanggalBeli = LocalDateTime.now();
        List<PesananItem> items = cartItems.stream()
                .map(cart -> {
                    Produk produk = cart.getProduk();
                    
                    // Validate and reduce stock
                    if (produk.getStock() < cart.getQuantity()) {
                        throw new RuntimeException("Stok tidak cukup untuk produk: " + produk.getNama());
                    }
                    
                    int newStock = produk.getStock() - cart.getQuantity();
                    produk.setStock(newStock);
                    produkRepository.save(produk);
                    
                    // Check if stock is low after purchase and send notification
                    if (newStock < 5 && newStock > 0) {
                        logger.info("Stock for product {} is low after purchase ({}), sending notification", 
                                produk.getNama(), newStock);
                        try {
                            notificationService.sendLowStockNotification(produk);
                        } catch (Exception e) {
                            logger.error("Failed to send low stock notification for product {}: {}", 
                                    produk.getId(), e.getMessage());
                        }
                    }
                    
                    // Update terjual_produk counter
                    TerjualProduk terjualProduk = terjualProdukRepository.findByProdukId(produk.getId())
                            .orElse(TerjualProduk.builder()
                                    .produk(produk)
                                    .terjual(0)
                                    .build());
                    terjualProduk.setTerjual(terjualProduk.getTerjual() + cart.getQuantity());
                    terjualProdukRepository.save(terjualProduk);
                    
                    return PesananItem.builder()
                            .pesanan(pesanan)
                            .produk(produk)
                            .quantity(cart.getQuantity())
                            .hargaSatuan(produk.getHarga())
                            .subtotal(produk.getHarga() * cart.getQuantity())
                            .karatEmas(produk.getKaratEmas())
                            .build();
                })
                .collect(Collectors.toList());
        
        pesanan.setItems(items);
        
        // Save order
        Pesanan savedPesanan = pesananRepository.save(pesanan);
        logger.info("Order created: {}", savedPesanan.getNomorPesanan());
        
        // Save to produk_terjual AFTER pesanan is saved
        for (Keranjang cart : cartItems) {
            Produk produk = cart.getProduk();
            ProdukTerjual produkTerjual = ProdukTerjual.builder()
                    .produk(produk)
                    .user(user)
                    .qty(cart.getQuantity())
                    .hargaSaatBeli(produk.getHarga())
                    .total(produk.getHarga() * cart.getQuantity())
                    .tanggalBeli(tanggalBeli)
                    .pesanan(savedPesanan)
                    .build();
            produkTerjualRepository.save(produkTerjual);
        }
        
        // Clear cart
        keranjangRepository.deleteByUserId(user.getId());
        
        logger.info("Order created successfully: {}", nomorPesanan);
        return convertToResponse(savedPesanan);
    }
    
    @Transactional
    public PesananResponse updateStatus(Long pesananId, OrderStatus newStatus, String nomorResi) {
        logger.info("Updating order {} to status: {}", pesananId, newStatus);
        
        Pesanan pesanan = pesananRepository.findById(pesananId)
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));
        
        LocalDateTime now = LocalDateTime.now();
        pesanan.setStatus(newStatus);
        pesanan.setUpdatedAt(now);
        
        switch (newStatus) {
            case DIKEMAS:
                pesanan.setDikemasAt(now);
                break;
            case DIKIRIM:
                pesanan.setDikirimAt(now);
                if (nomorResi != null && !nomorResi.isEmpty()) {
                    pesanan.setNomorResi(nomorResi);
                } else {
                    // Generate auto resi if not provided
                    pesanan.setNomorResi(generateNomorResi());
                }
                break;
            case SELESAI:
                pesanan.setSelesaiAt(now);
                break;
            default:
                break;
        }
        
        Pesanan updated = pesananRepository.save(pesanan);
        logger.info("Order status updated successfully");
        
        // Send notification to user for DIKIRIM or SELESAI status
        try {
            notificationService.sendOrderStatusNotification(updated, newStatus);
        } catch (Exception e) {
            logger.error("Failed to send order status notification: {}", e.getMessage());
        }
        
        return convertToResponse(updated);
    }
    
    @Transactional(readOnly = true)
    public List<PesananResponse> getPesananByUser(Long userId) {
        return pesananRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Long getTotalPesanan() {
        return pesananRepository.count();
    }
    
    @Transactional(readOnly = true)
    public List<PesananResponse> getAllPesanan() {
        return pesananRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PesananResponse getPesananByNomor(String nomorPesanan) {
        Pesanan pesanan = pesananRepository.findByNomorPesanan(nomorPesanan)
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));
        return convertToResponse(pesanan);
    }
    
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD-" + timestamp;
    }
    
    private String generateNomorResi() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        return String.format("RESI-%s-%04d", timestamp, random);
    }
    
    private PesananResponse convertToResponse(Pesanan pesanan) {
        List<PesananItemResponse> items = pesanan.getItems().stream()
                .map(item -> PesananItemResponse.builder()
                        .id(item.getId())
                        .produkId(item.getProduk().getId())
                        .namaProduk(item.getProduk().getNama())
                        .gambarProduk(item.getProduk().getGambar())
                        .quantity(item.getQuantity())
                        .hargaSatuan(item.getHargaSatuan())
                        .subtotal(item.getSubtotal())
                        .karatEmas(item.getKaratEmas())
                        .build())
                .collect(Collectors.toList());
        
        AlamatResponse alamatResponse = null;
        if (pesanan.getAlamat() != null) {
            Alamat a = pesanan.getAlamat();
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
        
        return PesananResponse.builder()
                .id(pesanan.getId())
                .nomorPesanan(pesanan.getNomorPesanan())
                .userId(pesanan.getUser().getId())
                .userName(pesanan.getUser().getEmail())
                .totalHarga(pesanan.getTotalHarga())
                .status(pesanan.getStatus())
                .alamat(alamatResponse)
                .catatan(pesanan.getCatatan())
                .nomorResi(pesanan.getNomorResi())
                .createdAt(pesanan.getCreatedAt())
                .dikemasAt(pesanan.getDikemasAt())
                .dikirimAt(pesanan.getDikirimAt())
                .selesaiAt(pesanan.getSelesaiAt())
                .items(items)
                .build();
    }
}
