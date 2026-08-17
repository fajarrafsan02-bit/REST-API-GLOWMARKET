package com.projekfajar.pesanan.model;

import java.math.BigDecimal;
import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.payment.model.Payment;
import com.projekfajar.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pesanan", indexes = {
        @Index(name = "idx_pesanan_user", columnList = "user_id"),
        @Index(name = "idx_pesanan_status", columnList = "status"),
        @Index(name = "idx_pesanan_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pesanan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nomorPesanan;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @OneToMany(mappedBy = "pesanan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PesananItem> items = new ArrayList<>();
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalHarga;

    @Column(precision = 19, scale = 2)
    private BigDecimal ongkir;

    /** Potongan voucher diskon (disalin dari Payment saat pesanan dibuat). */
    @Column(precision = 19, scale = 2)
    private BigDecimal diskon;

    /** Kode voucher yang dipakai (disalin dari Payment saat pesanan dibuat). */
    @Column(length = 50)
    private String kodeVoucher;

    /** RAJAONGKIR / TARIF_TETAP / GRATIS_MINIMAL_BELANJA — disalin dari Payment saat pesanan dibuat. */
    private String ongkirSumber;

    /** Disalin dari Payment — riwayat "dikirim via JNE REG", tidak mengubah nominal. */
    @Column(length = 50)
    private String ongkirKurir;

    @Column(length = 30)
    private String ongkirLayanan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    @ManyToOne
    @JoinColumn(name = "alamat_id")
    private Alamat alamat;

    /** Salinan alamat saat pesanan dibuat, agar riwayat tidak berubah. */
    @Embedded
    private AlamatSnapshot alamatSnapshot;
    
    private String catatan;
    
    private String nomorResi;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime dikemasAt;
    
    private LocalDateTime dikirimAt;
    
    private LocalDateTime selesaiAt;
    
    private LocalDateTime updatedAt;
}
