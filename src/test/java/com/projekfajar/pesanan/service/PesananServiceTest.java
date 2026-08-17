package com.projekfajar.pesanan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import com.projekfajar.akuntansi.service.PostingPenjualanService;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.keranjang.model.Keranjang;
import com.projekfajar.keranjang.repository.KeranjangRepository;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.payment.model.Payment;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.model.PesananItem;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.poin.service.PoinLoyalitasService;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.produk.repository.ProdukVarianRepository;
import com.projekfajar.restock.service.RestockNotifikasiService;
import com.projekfajar.terjual.model.ProdukTerjual;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.terjual.repository.ProdukTerjualRepository;
import com.projekfajar.tracking.service.TrackingService;
import com.projekfajar.user.model.User;

/**
 * Menguji titik paling berisiko pada alur pesanan: reservasi stok saat
 * checkout, perpindahan status saat lunas, dan pengembalian stok saat batal.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PesananServiceTest {

    @Mock
    private PesananRepository pesananRepository;
    @Mock
    private KeranjangRepository keranjangRepository;
    @Mock
    private ProdukRepository produkRepository;
    @Mock
    private ProdukTerjualRepository produkTerjualRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SettingService settingService;

    /** Email pesanan dikirim lewat event setelah commit, jadi cukup dipalsukan di sini. */
    @Mock
    private ApplicationEventPublisher eventPublisher;

    /** Isi jurnalnya diuji tersendiri di PostingPenjualanServiceTest. */
    @Mock
    private PostingPenjualanService postingPenjualanService;

    /*
     * Kolaborator berikut hanya "ikut terpanggil" saat stok dipulihkan atau
     * pesanan dibayar — perilakunya diuji di kelas tesnya masing-masing.
     * Tanpa dipalsukan di sini, @InjectMocks meninggalkannya null dan
     * pembatalan pesanan gagal dengan NullPointerException.
     */
    @Mock
    private ProdukVarianRepository varianRepository;
    @Mock
    private TrackingService trackingService;
    @Mock
    private RestockNotifikasiService restockNotifikasiService;
    @Mock
    private PoinLoyalitasService poinLoyalitasService;

    @InjectMocks
    private PesananService pesananService;

    private User user;
    private Produk produk;
    private Payment payment;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("pembeli@test.com").namaLengkap("Pembeli").build();

        produk = Produk.builder()
                .id(10L)
                .nama("Cincin Emas")
                .harga(new BigDecimal("1000000.00"))
                .hargaModal(new BigDecimal("700000.00"))
                .stock(5)
                .karatEmas(18)
                .build();

        payment = Payment.builder()
                .id(100L)
                .externalId("INV-TEST")
                .amount(new BigDecimal("2000000.00"))
                .ongkir(BigDecimal.ZERO)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(settingService.getInt(anyString(), anyInt())).thenReturn(5);
        when(pesananRepository.save(any(Pesanan.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Keranjang cartItem(int quantity) {
        return Keranjang.builder().id(1L).user(user).produk(produk).quantity(quantity).build();
    }

    @Test
    @DisplayName("Checkout memotong stok dan mengosongkan keranjang")
    void createPendingOrderReservesStock() {
        when(pesananRepository.existsByPaymentId(100L)).thenReturn(false);
        when(keranjangRepository.findByUserId(1L)).thenReturn(List.of(cartItem(2)));

        pesananService.createPendingOrder(payment);

        assertThat(produk.getStock()).isEqualTo(3); // 5 - 2
        verify(keranjangRepository).deleteByUserId(1L);

        ArgumentCaptor<Pesanan> captor = ArgumentCaptor.forClass(Pesanan.class);
        verify(pesananRepository).save(captor.capture());

        Pesanan saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(1);
        // Subtotal harus 1.000.000 x 2, dihitung dengan BigDecimal
        assertThat(saved.getItems().get(0).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("2000000"));
        // Nama produk ikut disalin agar riwayat tidak berubah
        assertThat(saved.getItems().get(0).getNamaProduk()).isEqualTo("Cincin Emas");
    }

    @Test
    @DisplayName("Harga modal ikut disalin saat checkout, perubahan modal setelahnya tidak mengubah pesanan")
    void createPendingOrderSnapshotsHargaModal() {
        when(pesananRepository.existsByPaymentId(100L)).thenReturn(false);
        when(keranjangRepository.findByUserId(1L)).thenReturn(List.of(cartItem(2)));

        pesananService.createPendingOrder(payment);

        ArgumentCaptor<Pesanan> captor = ArgumentCaptor.forClass(Pesanan.class);
        verify(pesananRepository).save(captor.capture());
        PesananItem item = captor.getValue().getItems().get(0);

        assertThat(item.getHargaModal()).isEqualByComparingTo(new BigDecimal("700000"));

        // Admin menaikkan harga modal setelah pesanan terbentuk
        produk.setHargaModal(new BigDecimal("950000.00"));

        // HPP pesanan lama tetap memakai angka saat barang terjual
        assertThat(item.getHargaModal()).isEqualByComparingTo(new BigDecimal("700000"));
    }

    @Test
    @DisplayName("Checkout ditolak bila stok tidak mencukupi, stok tidak berubah")
    void createPendingOrderRejectsWhenStockInsufficient() {
        when(pesananRepository.existsByPaymentId(100L)).thenReturn(false);
        when(keranjangRepository.findByUserId(1L)).thenReturn(List.of(cartItem(99)));

        assertThatThrownBy(() -> pesananService.createPendingOrder(payment))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stok tidak cukup");

        assertThat(produk.getStock()).isEqualTo(5);
        verify(keranjangRepository, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("Pembayaran lunas memindahkan pesanan PENDING ke DIKEMAS")
    void markOrderPaidMovesPendingToDikemas() {
        Pesanan pending = pendingOrder();
        when(pesananRepository.findByPaymentId(100L)).thenReturn(Optional.of(pending));

        pesananService.markOrderPaid(payment);

        assertThat(pending.getStatus()).isEqualTo(OrderStatus.DIKEMAS);
        assertThat(pending.getDikemasAt()).isNotNull();
        verify(produkTerjualRepository).save(any());
        // Pembukuan berjalan dalam transaksi yang sama dengan perubahan status
        verify(postingPenjualanService).catatPenjualan(pending);
    }

    @Test
    @DisplayName("Pembayaran lunas yang diproses dua kali tidak menggandakan catatan penjualan")
    void markOrderPaidIsIdempotent() {
        Pesanan sudahDikemas = pendingOrder();
        sudahDikemas.setStatus(OrderStatus.DIKEMAS);
        when(pesananRepository.findByPaymentId(100L)).thenReturn(Optional.of(sudahDikemas));

        pesananService.markOrderPaid(payment);

        verify(produkTerjualRepository, never()).save(any());
        verify(postingPenjualanService, never()).catatPenjualan(any());
    }

    @Test
    @DisplayName("Invoice kedaluwarsa mengembalikan stok yang tadi ditahan")
    void cancelUnpaidOrderRestoresStock() {
        Pesanan pending = pendingOrder();
        produk.setStock(3); // sisa setelah 2 unit direservasi
        when(pesananRepository.findByPaymentId(100L)).thenReturn(Optional.of(pending));

        pesananService.cancelUnpaidOrder(payment, "invoice kedaluwarsa");

        assertThat(produk.getStock()).isEqualTo(5);
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.DIBATALKAN);
    }

    @Test
    @DisplayName("Pesanan yang sudah dibayar tidak ikut dibatalkan dan stoknya tidak ditambah")
    void cancelUnpaidOrderIgnoresPaidOrder() {
        Pesanan dikemas = pendingOrder();
        dikemas.setStatus(OrderStatus.DIKEMAS);
        produk.setStock(3);
        when(pesananRepository.findByPaymentId(100L)).thenReturn(Optional.of(dikemas));

        pesananService.cancelUnpaidOrder(payment, "invoice kedaluwarsa");

        assertThat(produk.getStock()).isEqualTo(3);
        assertThat(dikemas.getStatus()).isEqualTo(OrderStatus.DIKEMAS);
    }

    @Test
    @DisplayName("Pesanan lunas yang dibatalkan admin: stok kembali, catatan penjualan dicabut, jurnal dibalik")
    void cancelPaidOrderReversesEverything() {
        Pesanan dikemas = pendingOrder();
        dikemas.setStatus(OrderStatus.DIKEMAS);
        produk.setStock(3); // sisa setelah 2 unit terjual

        when(pesananRepository.findById(50L)).thenReturn(Optional.of(dikemas));
        when(produkTerjualRepository.findByPesananId(50L))
                .thenReturn(List.of(ProdukTerjual.builder().id(1L).produk(produk).qty(2).build()));

        pesananService.updateStatus(50L, OrderStatus.DIBATALKAN, null);

        assertThat(produk.getStock()).isEqualTo(5);
        assertThat(dikemas.getStatus()).isEqualTo(OrderStatus.DIBATALKAN);
        verify(produkTerjualRepository).deleteAll(anyList());
        verify(postingPenjualanService).batalkanPenjualan(eq(dikemas), anyString());
    }

    @Test
    @DisplayName("Pesanan lunas yang dikirim tidak menyentuh pembukuan penjualan")
    void shippingPaidOrderDoesNotTouchJournal() {
        Pesanan dikemas = pendingOrder();
        dikemas.setStatus(OrderStatus.DIKEMAS);
        when(pesananRepository.findById(50L)).thenReturn(Optional.of(dikemas));

        pesananService.updateStatus(50L, OrderStatus.DIKIRIM, "RESI123");

        assertThat(dikemas.getStatus()).isEqualTo(OrderStatus.DIKIRIM);
        verify(postingPenjualanService, never()).batalkanPenjualan(any(), anyString());
        verify(produkTerjualRepository, never()).deleteAll(anyList());
    }

    private Pesanan pendingOrder() {
        Pesanan pesanan = Pesanan.builder()
                .id(50L)
                .nomorPesanan("ORD-TEST")
                .user(user)
                .payment(payment)
                .totalHarga(new BigDecimal("2000000.00"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        pesanan.setItems(List.of(PesananItem.builder()
                .id(1L)
                .pesanan(pesanan)
                .produk(produk)
                .namaProduk(produk.getNama())
                .quantity(2)
                .hargaSatuan(produk.getHarga())
                .subtotal(new BigDecimal("2000000.00"))
                .build()));

        return pesanan;
    }
}
