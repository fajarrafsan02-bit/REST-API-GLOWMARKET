package com.projekfajar.pesanan.service;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;

import java.math.BigDecimal;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.alamat.dto.AlamatResponse;
import com.projekfajar.pesanan.dto.PesananItemResponse;
import com.projekfajar.pesanan.dto.PesananResponse;
import com.projekfajar.payment.dto.PaymentResponse;
import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.keranjang.model.Keranjang;
import com.projekfajar.payment.model.Payment;
import com.projekfajar.pesanan.model.AlamatSnapshot;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.model.PesananItem;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.terjual.model.ProdukTerjual;
import com.projekfajar.user.model.User;
import com.projekfajar.keranjang.repository.KeranjangRepository;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.terjual.repository.ProdukTerjualRepository;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.akuntansi.service.PostingPenjualanService;
import com.projekfajar.notification.event.OrderEmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PesananService {

    private final PesananRepository pesananRepository;
    private final KeranjangRepository keranjangRepository;
    private final UserRepository userRepository;
    private final com.projekfajar.produk.repository.ProdukRepository produkRepository;
    private final com.projekfajar.produk.repository.ProdukVarianRepository varianRepository;
    private final ProdukTerjualRepository produkTerjualRepository;
    private final NotificationService notificationService;
    private final SettingService settingService;
    private final ApplicationEventPublisher eventPublisher;
    private final PostingPenjualanService postingPenjualanService;
    private final com.projekfajar.tracking.service.TrackingService trackingService;
    private final com.projekfajar.pengembalian.repository.PengembalianRepository pengembalianRepository;
    private final com.projekfajar.restock.service.RestockNotifikasiService restockNotifikasiService;
    private final com.projekfajar.poin.service.PoinLoyalitasService poinLoyalitasService;
    
    /**
     * Dipanggil saat invoice dibuat (checkout), sebelum pembayaran.
     *
     * Pesanan langsung dibuat dengan status PENDING dan stok direservasi di sini,
     * supaya dua pembeli tidak bisa membayar produk terakhir yang sama. Isi pesanan
     * juga di-snapshot dari keranjang saat ini, jadi perubahan keranjang setelah
     * checkout tidak lagi mengubah pesanan yang sudah dibayar.
     */
    @Transactional
    public PesananResponse createPendingOrder(Payment payment) {
        log.info("Creating pending order for paymentId={} amount={}", payment.getId(), payment.getAmount());
        if (pesananRepository.existsByPaymentId(payment.getId())) {
            log.warn("Pending order already exists for paymentId={}, returning existing order", payment.getId());
            return convertToResponse(pesananRepository.findByPaymentId(payment.getId()).orElseThrow());
        }

        User user = payment.getUser();
        if (user == null) {
            log.warn("Payment {} has no related user, cannot create order", payment.getId());
            throw new BusinessException("Payment tidak terkait dengan user");
        }

        List<Keranjang> cartItems = keranjangRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            log.warn("Cart is empty for userId={}, cannot create order for paymentId={}",
                    user.getId(), payment.getId());
            throw new BusinessException("Keranjang kosong, tidak bisa membuat pesanan");
        }

        Pesanan pesanan = Pesanan.builder()
                .nomorPesanan(generateOrderNumber())
                .user(user)
                .payment(payment)
                .totalHarga(payment.getAmount())
                .ongkir(payment.getOngkir())
                .diskon(payment.getDiskon())
                .kodeVoucher(payment.getKodeVoucher())
                .ongkirSumber(payment.getOngkirSumber())
                .ongkirKurir(payment.getOngkirKurir())
                .ongkirLayanan(payment.getOngkirLayanan())
                .status(OrderStatus.PENDING)
                .alamat(payment.getAlamat())
                .alamatSnapshot(buildAlamatSnapshot(payment.getAlamat()))
                .catatan(payment.getCatatan())
                .createdAt(LocalDateTime.now())
                .build();

        int lowStockThreshold = settingService.getInt("lowStock.threshold", 5);
        List<PesananItem> items = new ArrayList<>();

        for (Keranjang cart : cartItems) {
            Produk produk = cart.getProduk();
            var varian = cart.getVariant();

            int stok = varian != null
                    ? (varian.getStock() != null ? varian.getStock() : 0)
                    : (produk.getStock() != null ? produk.getStock() : 0);
            BigDecimal harga = varian != null ? varian.getHarga() : produk.getHarga();

            if (stok < cart.getQuantity()) {
                log.warn("Insufficient stock for productId={} name={} requested={} available={}",
                        produk.getId(), produk.getNama(), cart.getQuantity(), stok);
                throw new BusinessException("Stok tidak cukup untuk produk: " + produk.getNama());
            }

            int newStock = stok - cart.getQuantity();
            if (varian != null) {
                varian.setStock(newStock);
                varianRepository.save(varian);
            } else {
                produk.setStock(newStock);
                produkRepository.save(produk);
            }
            log.debug("Stock reserved for productId={} qty={} remaining={}",
                    produk.getId(), cart.getQuantity(), newStock);

            if (newStock < lowStockThreshold && newStock > 0) {
                try {
                    notificationService.sendLowStockNotification(produk);
                } catch (Exception e) {
                    log.error("Failed to send low stock notification for productId={}: {}",
                            produk.getId(), e.getMessage(), e);
                }
            }

            items.add(PesananItem.builder()
                    .pesanan(pesanan)
                    .produk(produk)
                    .variant(varian)
                    .namaVariant(varian != null ? varian.getNama() : null)
                    .namaProduk(produk.getNama())
                    .gambarProduk(produk.getGambar())
                    .quantity(cart.getQuantity())
                    .hargaSatuan(harga)
                    .hargaModal(varian != null ? varian.getHargaModal() : produk.getHargaModal())
                    .subtotal(harga.multiply(BigDecimal.valueOf(cart.getQuantity())))
                    .karatEmas(produk.getKaratEmas())
                    .build());
        }

        pesanan.setItems(items);
        Pesanan saved = pesananRepository.save(pesanan);

        // Keranjang dikosongkan di sini karena isinya sudah dipindahkan ke pesanan
        keranjangRepository.deleteByUserId(user.getId());

        log.info("Pending order created: nomor={} orderId={} paymentId={} items={} total={}",
                saved.getNomorPesanan(), saved.getId(), payment.getId(), items.size(), saved.getTotalHarga());
        return convertToResponse(saved);
    }

    /**
     * Dipanggil saat pembayaran lunas. Stok sudah dipotong sejak checkout,
     * jadi di sini hanya status yang berpindah dan catatan penjualan ditulis.
     */
    @Transactional
    public PesananResponse markOrderPaid(Payment payment) {
        log.info("Marking order as paid for paymentId={} amount={}", payment.getId(), payment.getAmount());
        Pesanan pesanan = pesananRepository.findByPaymentId(payment.getId()).orElse(null);

        if (pesanan == null) {
            // Invoice lama yang dibuat sebelum alur pesanan PENDING ada.
            log.warn("No PENDING order found for paymentId={}, falling back to legacy flow", payment.getId());
            return createOrderFromPayment(payment);
        }

        if (pesanan.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} already processed (status={}), skipping", 
                    pesanan.getNomorPesanan(), pesanan.getStatus());
            return convertToResponse(pesanan);
        }

        LocalDateTime now = LocalDateTime.now();
        pesanan.setStatus(OrderStatus.DIKEMAS);
        pesanan.setDikemasAt(now);
        pesanan.setUpdatedAt(now);

        Pesanan saved = pesananRepository.save(pesanan);
        recordSoldItems(saved);

        // Jurnal dicatat dalam transaksi yang sama: bila pembukuan gagal, status
        // pesanan ikut batal dan webhook Xendit akan mengulang — tidak akan ada
        // penjualan lunas yang tidak terbukukan.
        postingPenjualanService.catatPenjualan(saved);

        // Poin loyalitas dari pembelian yang sudah lunas. Ini jalur normal
        // (pesanan dibuat PENDING saat checkout, lalu lunas di sini) — gagal
        // memberi poin tidak boleh menggagalkan status pesanan yang sudah
        // benar-benar dibayar, jadi dibungkus try-catch seperti jalur legacy
        // di createOrderFromPayment.
        try {
            poinLoyalitasService.tambahPoinDariPembelian(saved);
        } catch (Exception e) {
            log.error("Failed to record loyalty points for orderId={}: {}",
                    saved.getId(), e.getMessage(), e);
        }

        // Bukti pembayaran dikirim setelah transaksi commit
        eventPublisher.publishEvent(OrderEmailEvent.pembayaranLunas(saved.getId()));

        log.info("Order marked as paid: nomor={} orderId={} status={} total={}",
                saved.getNomorPesanan(), saved.getId(), saved.getStatus(), saved.getTotalHarga());
        return convertToResponse(saved);
    }

    /**
     * Membatalkan pesanan yang belum dibayar (invoice kedaluwarsa/gagal)
     * dan mengembalikan stok yang tadi direservasi.
     */
    @Transactional
    public void cancelUnpaidOrder(Payment payment, String alasan) {
        log.info("Cancelling unpaid order for paymentId={} reason={}", payment.getId(), alasan);
        Pesanan pesanan = pesananRepository.findByPaymentId(payment.getId()).orElse(null);

        if (pesanan == null || pesanan.getStatus() != OrderStatus.PENDING) {
            log.debug("No PENDING order to cancel for paymentId={}", payment.getId());
            return;
        }

        restoreStock(pesanan);

        pesanan.setStatus(OrderStatus.DIBATALKAN);
        pesanan.setUpdatedAt(LocalDateTime.now());
        pesananRepository.save(pesanan);

        eventPublisher.publishEvent(OrderEmailEvent.perubahanStatus(
                pesanan.getId(), OrderStatus.DIBATALKAN, true));

        log.info("Order cancelled: nomor={} reason={}, stock restored", pesanan.getNomorPesanan(), alasan);
    }

    /** Status yang berarti uang pembeli sudah masuk. */
    private boolean sudahDibayar(OrderStatus status) {
        return status == OrderStatus.DIKEMAS
                || status == OrderStatus.DIKIRIM
                || status == OrderStatus.SELESAI
                || status == OrderStatus.DIKEMBALIKAN;
    }

    /**
     * Mencabut catatan penjualan sebuah pesanan yang batal.
     *
     * Baris ini adalah sumber angka "terjual" pada katalog dan statistik, jadi
     * kalau ditinggalkan produk akan tampak laku padahal transaksinya tidak jadi.
     */
    private void hapusCatatanPenjualan(Pesanan pesanan) {
        List<ProdukTerjual> catatan = produkTerjualRepository.findByPesananId(pesanan.getId());

        if (!catatan.isEmpty()) {
            produkTerjualRepository.deleteAll(catatan);
            log.info("Revoked {} sales records for order nomor={}",
                    catatan.size(), pesanan.getNomorPesanan());
        }
    }

    /** Mengembalikan stok yang direservasi oleh sebuah pesanan. */
    private void restoreStock(Pesanan pesanan) {
        log.debug("Restoring stock for order nomor={} items={}",
                pesanan.getNomorPesanan(), pesanan.getItems().size());
        for (PesananItem item : pesanan.getItems()) {
            if (item.getVariant() != null) {
                var varian = item.getVariant();
                varian.setStock(varian.getStock() + item.getQuantity());
                varianRepository.save(varian);
                restockNotifikasiService.cekRestock(varian.getProduk());
            } else {
                Produk produk = item.getProduk();
                produk.setStock(produk.getStock() + item.getQuantity());
                produkRepository.save(produk);
                restockNotifikasiService.cekRestock(produk);
            }
        }
    }

    /**
     * Menulis catatan penjualan per transaksi.
     * Hanya dipanggil setelah pembayaran benar-benar lunas.
     *
     * Jumlah terjual per produk tidak lagi disimpan terpisah — angkanya
     * dihitung dari catatan ini, sehingga tidak ada dua sumber yang bisa beda.
     */
    private void recordSoldItems(Pesanan pesanan) {
        LocalDateTime tanggalBeli = LocalDateTime.now();

        for (PesananItem item : pesanan.getItems()) {
            Produk produk = item.getProduk();

            produkTerjualRepository.save(ProdukTerjual.builder()
                    .produk(produk)
                    .user(pesanan.getUser())
                    .qty(item.getQuantity())
                    .hargaSaatBeli(item.getHargaSatuan())
                    .total(item.getSubtotal())
                    .tanggalBeli(tanggalBeli)
                    .pesanan(pesanan)
                    .build());
        }

        log.debug("Recorded {} sold items for order nomor={}",
                pesanan.getItems().size(), pesanan.getNomorPesanan());
    }

    /**
     * Jalur lama: membangun pesanan dari keranjang saat pembayaran lunas.
     * Dipertahankan hanya untuk invoice yang dibuat sebelum alur pesanan PENDING.
     */
    @Transactional
    public PesananResponse createOrderFromPayment(Payment payment) {
        log.info("Creating order from paymentId={} amount={} (legacy flow)", payment.getId(), payment.getAmount());
        
        // Check if order already exists
        if (pesananRepository.existsByPaymentId(payment.getId())) {
            log.warn("Order already exists for paymentId={}", payment.getId());
            return convertToResponse(pesananRepository.findByPaymentId(payment.getId()).orElseThrow());
        }
        
        User user = payment.getUser();
        if (user == null) {
            log.warn("Payment {} has no related user, cannot create order", payment.getId());
            throw new BusinessException("Payment tidak terkait dengan user");
        }
        
        // Get cart items
        List<Keranjang> cartItems = keranjangRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            log.warn("Cart is empty for userId={}, cannot create order for paymentId={}",
                    user.getId(), payment.getId());
            throw new BusinessException("Keranjang kosong, tidak bisa membuat pesanan");
        }
        
        // Generate order number
        String nomorPesanan = generateOrderNumber();
        
        // Create order
        Pesanan pesanan = Pesanan.builder()
                .nomorPesanan(nomorPesanan)
                .user(user)
                .payment(payment)
                .totalHarga(payment.getAmount())
                .ongkir(payment.getOngkir())
                .diskon(payment.getDiskon())
                .kodeVoucher(payment.getKodeVoucher())
                .ongkirSumber(payment.getOngkirSumber())
                .ongkirKurir(payment.getOngkirKurir())
                .ongkirLayanan(payment.getOngkirLayanan())
                .status(OrderStatus.DIKEMAS) // Auto set to DIKEMAS when paid
                .alamat(payment.getAlamat())
                .alamatSnapshot(buildAlamatSnapshot(payment.getAlamat()))
                .catatan(payment.getCatatan())
                .createdAt(LocalDateTime.now())
                .dikemasAt(LocalDateTime.now())
                .build();
        
        // Create order items from cart and reduce stock
        LocalDateTime tanggalBeli = LocalDateTime.now();
        int lowStockThreshold = settingService.getInt("lowStock.threshold", 5);
        List<PesananItem> items = cartItems.stream()
                .map(cart -> {
                    Produk produk = cart.getProduk();
                    var varian = cart.getVariant();

                    int stok = varian != null
                            ? (varian.getStock() != null ? varian.getStock() : 0)
                            : (produk.getStock() != null ? produk.getStock() : 0);
                    BigDecimal harga = varian != null ? varian.getHarga() : produk.getHarga();

                    // Validate and reduce stock
                    if (stok < cart.getQuantity()) {
                        log.warn("Insufficient stock for productId={} name={} requested={} available={}",
                                produk.getId(), produk.getNama(), cart.getQuantity(), stok);
                        throw new BusinessException("Stok tidak cukup untuk produk: " + produk.getNama());
                    }

                    int newStock = stok - cart.getQuantity();
                    if (varian != null) {
                        varian.setStock(newStock);
                        varianRepository.save(varian);
                    } else {
                        produk.setStock(newStock);
                        produkRepository.save(produk);
                    }

                    // Check if stock is low after purchase and send notification
                    if (newStock < lowStockThreshold && newStock > 0) {
                        log.info("Low stock after purchase: productId={} name={} remaining={}",
                                produk.getId(), produk.getNama(), newStock);
                        try {
                            notificationService.sendLowStockNotification(produk);
                        } catch (Exception e) {
                            log.error("Failed to send low stock notification for productId={}: {}",
                                    produk.getId(), e.getMessage(), e);
                        }
                    }

                    return PesananItem.builder()
                            .pesanan(pesanan)
                            .produk(produk)
                            .variant(varian)
                            .namaVariant(varian != null ? varian.getNama() : null)
                            .namaProduk(produk.getNama())
                            .gambarProduk(produk.getGambar())
                            .quantity(cart.getQuantity())
                            .hargaSatuan(harga)
                            .hargaModal(varian != null ? varian.getHargaModal() : produk.getHargaModal())
                            .subtotal(harga.multiply(BigDecimal.valueOf(cart.getQuantity())))
                            .karatEmas(produk.getKaratEmas())
                            .build();
                })
                .collect(Collectors.toList());
        
        pesanan.setItems(items);
        
        // Save order
        Pesanan savedPesanan = pesananRepository.save(pesanan);
        log.debug("Order persisted: nomor={} orderId={}", savedPesanan.getNomorPesanan(), savedPesanan.getId());
        
        recordSoldItems(savedPesanan);

        // Jalur ini juga penjualan lunas, jadi pembukuannya harus sama dengan
        // jalur normal — kalau tidak, ada uang masuk yang tidak pernah dijurnal.
        postingPenjualanService.catatPenjualan(savedPesanan);

        // Poin loyalitas dari pembelian yang sudah lunas.
        try {
            poinLoyalitasService.tambahPoinDariPembelian(savedPesanan);
        } catch (Exception e) {
            log.error("Failed to record loyalty points for orderId={}: {}",
                    savedPesanan.getId(), e.getMessage(), e);
        }

        // Clear cart
        keranjangRepository.deleteByUserId(user.getId());
        
        log.info("Order created successfully: nomor={} orderId={} items={} total={}",
                nomorPesanan, savedPesanan.getId(), items.size(), savedPesanan.getTotalHarga());
        return convertToResponse(savedPesanan);
    }
    
    @Transactional
    public PesananResponse updateStatus(Long pesananId, OrderStatus newStatus, String nomorResi) {
        log.info("Updating orderId={} to status={} resi={}", pesananId, newStatus, nomorResi);
        
        Pesanan pesanan = pesananRepository.findById(pesananId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));

        // Pesanan yang belum dibayar tidak boleh dikemas/dikirim/diselesaikan —
        // satu-satunya perpindahan yang masuk akal adalah pembatalan.
        if (pesanan.getStatus() == OrderStatus.PENDING && newStatus != OrderStatus.DIBATALKAN) {
            log.warn("Rejected status change for unpaid orderId={} from PENDING to {}", pesananId, newStatus);
            throw new BusinessException(
                    "Pesanan ini belum dibayar, statusnya hanya bisa diubah menjadi Dibatalkan");
        }

        // Stok yang direservasi saat checkout dikembalikan bila pesanan
        // dibatalkan sebelum sempat dibayar.
        if (pesanan.getStatus() == OrderStatus.PENDING && newStatus == OrderStatus.DIBATALKAN) {
            restoreStock(pesanan);
        }

        // Pesanan yang sudah dibayar lalu dibatalkan harus dibalik seutuhnya:
        // barang kembali ke stok, catatan penjualan dicabut, dan jurnalnya dibalik.
        // Kalau salah satu dilewatkan, laporan akan menghitung penjualan yang
        // sebenarnya tidak jadi.
        if (sudahDibayar(pesanan.getStatus()) && newStatus == OrderStatus.DIBATALKAN) {
            restoreStock(pesanan);
            hapusCatatanPenjualan(pesanan);
            postingPenjualanService.batalkanPenjualan(pesanan, "pesanan dibatalkan admin");
        }

        OrderStatus statusLama = pesanan.getStatus();
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
                    // Auto generate resi menyesuaikan format kurir pesanan.
                    pesanan.setNomorResi(generateNomorResi(pesanan.getOngkirKurir()));
                }
                break;
            case SELESAI:
                pesanan.setSelesaiAt(now);
                break;
            default:
                break;
        }
        
        Pesanan updated = pesananRepository.save(pesanan);
        log.info("Order status updated: nomor={} from={} to={} resi={}",
                updated.getNomorPesanan(), statusLama, newStatus, updated.getNomorResi());

        // Timeline tracking dibuat saat pesanan dikirim dan ditutup saat selesai.
        try {
            if (newStatus == OrderStatus.DIKIRIM) {
                trackingService.buatTrackingPesanan(updated, updated.getNomorResi());
            } else if (newStatus == OrderStatus.SELESAI) {
                trackingService.tutupDiterima(updated);
            }
        } catch (Exception e) {
            log.error("Failed to update tracking for orderId={}: {}", updated.getId(), e.getMessage(), e);
        }
        
        // Send notification to user for DIKIRIM or SELESAI status
        try {
            notificationService.sendOrderStatusNotification(updated, newStatus);
        } catch (Exception e) {
            log.error("Failed to send order status notification for orderId={}: {}",
                    updated.getId(), e.getMessage(), e);
        }

        // Email menyusul setelah transaksi ini commit (lihat OrderEmailListener)
        eventPublisher.publishEvent(OrderEmailEvent.perubahanStatus(
                updated.getId(), newStatus, statusLama == OrderStatus.PENDING));

        return convertToResponse(updated);
    }
    
    @Transactional(readOnly = true)
    public List<PesananResponse> getPesananByUser(Long userId) {
        log.debug("Fetching orders for userId={}", userId);
        List<PesananResponse> hasil = pesananRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        log.debug("Found {} orders for userId={}", hasil.size(), userId);
        return hasil;
    }
    
    @Transactional(readOnly = true)
    public Long getTotalPesanan() {
        // Hanya pesanan terbayar — pesanan PENDING/DIBATALKAN bukan penjualan.
        Long total = pesananRepository.countPesananTerbayar();
        log.debug("Total paid orders: {}", total);
        return total;
    }
    
    @Transactional(readOnly = true)
    public List<PesananResponse> getAllPesanan() {
        List<PesananResponse> hasil = pesananRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        log.debug("Fetched {} orders", hasil.size());
        return hasil;
    }
    
    @Transactional(readOnly = true)
    public PesananResponse getPesananByExternalId(String externalId) {
        log.debug("Fetching order by externalId={}", externalId);
        Pesanan pesanan = pesananRepository.findByPaymentExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));
        return convertToResponse(pesanan);
    }

    @Transactional(readOnly = true)
    public PesananResponse getPesananByNomor(String nomorPesanan) {
        log.debug("Fetching order by nomor={}", nomorPesanan);
        Pesanan pesanan = pesananRepository.findByNomorPesanan(nomorPesanan)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));
        return convertToResponse(pesanan);
    }
    
    private String generateOrderNumber() {
        // Suffix acak wajib: tanpa itu dua pesanan pada detik yang sama
        // menabrak unique constraint nomor_pesanan.
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }
    
    /**
     * Meniru format nomor resi masing-masing kurir agar tampilan lebih
     * realistis. Kurir tidak terdaftar tetap mendapat resi generik.
     */
    private String generateNomorResi(String kurir) {
        if (kurir == null) {
            kurir = "";
        }
        String k = kurir.trim().toLowerCase(Locale.ROOT);

        return switch (k) {
            case "jne" -> "JP" + angka(10);
            case "jnt", "j&t" -> "JT" + angka(10);
            case "sicepat", "si cepat" -> "SPXID" + angka(10);
            case "pos" -> "RP" + angka(8);
            case "tiki" -> "LD" + angka(9);
            case "id", "idexpress", "id express" -> "I" + angka(9);
            case "anteraja", "anter aja" -> "D" + angka(12);
            default -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                yield String.format("RESI-%s-%04d", timestamp, (int) (Math.random() * 10000));
            }
        };
    }

    private String angka(int jumlahDigit) {
        StringBuilder sb = new StringBuilder(jumlahDigit);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < jumlahDigit; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /** Menyalin alamat pengiriman untuk disimpan bersama pesanan. */
    private AlamatSnapshot buildAlamatSnapshot(Alamat alamat) {
        if (alamat == null) {
            return null;
        }

        return AlamatSnapshot.builder()
                .namaLengkap(alamat.getNamaLengkap())
                .nomorTelepon(alamat.getNomorTelepon())
                .alamatLengkap(alamat.getAlamatLengkap())
                .provinsi(alamat.getProvinsi())
                .kota(alamat.getKota())
                .kecamatan(alamat.getKecamatan())
                .kelurahan(alamat.getKelurahan())
                .kodePos(alamat.getKodePos())
                .build();
    }

    private PesananResponse convertToResponse(Pesanan pesanan) {
        List<PesananItemResponse> items = pesanan.getItems().stream()
                .map(item -> PesananItemResponse.builder()
                        .id(item.getId())
                        .produkId(item.getProduk().getId())
                        // Utamakan salinan saat pembelian; pesanan lama belum punya
                        // salinan ini sehingga jatuh kembali ke data produk saat ini.
                        .namaProduk(item.getNamaProduk() != null
                                ? item.getNamaProduk()
                                : item.getProduk().getNama())
                        .gambarProduk(item.getGambarProduk() != null
                                ? item.getGambarProduk()
                                : item.getProduk().getGambar())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .namaVariant(item.getNamaVariant() != null
                                ? item.getNamaVariant()
                                : (item.getVariant() != null ? item.getVariant().getNama() : null))
                        .quantity(item.getQuantity())
                        .hargaSatuan(item.getHargaSatuan())
                        .subtotal(item.getSubtotal())
                        .karatEmas(item.getKaratEmas())
                        .build())
                .collect(Collectors.toList());
        
        AlamatResponse alamatResponse = null;
        AlamatSnapshot snapshot = pesanan.getAlamatSnapshot();

        if (snapshot != null && snapshot.getAlamatLengkap() != null) {
            // Alamat sebagaimana adanya saat pesanan dibuat, meski alamat
            // aslinya sudah diubah atau dihapus pemiliknya.
            alamatResponse = AlamatResponse.builder()
                    .id(pesanan.getAlamat() != null ? pesanan.getAlamat().getId() : null)
                    .userId(pesanan.getUser().getId())
                    .namaLengkap(snapshot.getNamaLengkap())
                    .nomorTelepon(snapshot.getNomorTelepon())
                    .alamatLengkap(snapshot.getAlamatLengkap())
                    .provinsi(snapshot.getProvinsi())
                    .kota(snapshot.getKota())
                    .kecamatan(snapshot.getKecamatan())
                    .kelurahan(snapshot.getKelurahan())
                    .kodePos(snapshot.getKodePos())
                    .build();
        } else if (pesanan.getAlamat() != null) {
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

        OrderStatus statusResponse = pesanan.getStatus();
        if (pengembalianRepository != null && pesanan.getId() != null) {
            var optPengembalian = pengembalianRepository.findByPesananId(pesanan.getId());
            if (optPengembalian.isPresent()) {
                var p = optPengembalian.get();
                if (p.getStatus() != com.projekfajar.pengembalian.model.PengembalianStatus.DITOLAK) {
                    statusResponse = OrderStatus.DIKEMBALIKAN;
                }
            }
        }

        return PesananResponse.builder()
                .id(pesanan.getId())
                .nomorPesanan(pesanan.getNomorPesanan())
                .externalId(pesanan.getPayment() != null ? pesanan.getPayment().getExternalId() : null)
                .userId(pesanan.getUser().getId())
                .userName(pesanan.getUser().getEmail())
                .totalHarga(pesanan.getTotalHarga())
                .ongkir(pesanan.getOngkir())
                .diskon(pesanan.getDiskon())
                .kodeVoucher(pesanan.getKodeVoucher())
                .ongkirSumber(pesanan.getOngkirSumber())
                .ongkirKurir(pesanan.getOngkirKurir())
                .ongkirLayanan(pesanan.getOngkirLayanan())
                .status(statusResponse)
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
