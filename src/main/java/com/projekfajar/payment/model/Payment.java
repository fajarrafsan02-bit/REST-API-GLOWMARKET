package com.projekfajar.payment.model;

import java.math.BigDecimal;
import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_user", columnList = "user_id"),
        @Index(name = "idx_payment_status_expired", columnList = "status, expiredAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String externalId;
    
    @Column(nullable = false)
    private String xenditInvoiceId;
    
    @Column(nullable = false)
    private String invoiceUrl;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal ongkir;

    /** Potongan voucher diskon (0 bila tidak ada). */
    @Column(precision = 19, scale = 2)
    private BigDecimal diskon;

    /** Kode voucher yang dipakai (null bila tidak ada). */
    @Column(length = 50)
    private String kodeVoucher;

    /** RAJAONGKIR / TARIF_TETAP / GRATIS_MINIMAL_BELANJA — lihat OngkirCalculationService. */
    private String ongkirSumber;

    /** Kode kurir RajaOngkir yang benar-benar dipakai (nullable, tarif tetap tidak punya). */
    @Column(length = 50)
    private String ongkirKurir;

    /** Layanan kurir yang benar-benar dipakai, mis. REG / YES. */
    @Column(length = 30)
    private String ongkirLayanan;

    @Column(nullable = false)
    private String status; // PENDING, PAID, EXPIRED, FAILED
    
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String customerEmail;
    
    private String customerPhone;
    
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "alamat_id")
    private Alamat alamat;
    
    private String catatan;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    @Column(nullable = false)
    private LocalDateTime expiredAt;
    
    private LocalDateTime updatedAt;
}
