package com.projekfajar.voucher.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projekfajar.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kupon diskon yang dipakai pada checkout.
 *
 * Jenis PERSEN memotong persentase dari subtotal (dibatasi maksDiskon),
 * jenis NOMINAL memotong sejumlah rupiah. Keduanya hanya berlaku bila
 * subtotal pesanan mencapai minBelanja.
 */
@Entity
@Table(name = "voucher")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {

    public static final String JENIS_PERSEN = "PERSEN";
    public static final String JENIS_NOMINAL = "NOMINAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String kode;

    /** PERSEN / NOMINAL */
    @Column(nullable = false, length = 20)
    private String jenis;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal nilai;

    /** Subtotal minimum agar voucher berlaku (null = tanpa syarat). */
    @Column(precision = 19, scale = 2)
    private BigDecimal minBelanja;

    /** Batas maksimal diskon untuk voucher PERSEN (null = tanpa batas). */
    @Column(precision = 19, scale = 2)
    private BigDecimal maksDiskon;

    /** Jumlah pemakaian maksimal (null = tanpa batas). */
    private Integer kuota;

    @Column(nullable = false)
    @Builder.Default
    private Integer terpakai = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    private LocalDateTime berlakuDari;

    private LocalDateTime berlakuSampai;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    /**
     * Pemilik voucher. Null berarti voucher umum yang bisa dipakai siapa saja;
     * terisi untuk voucher hasil tukar poin yang hanya berlaku untuk pemiliknya.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
