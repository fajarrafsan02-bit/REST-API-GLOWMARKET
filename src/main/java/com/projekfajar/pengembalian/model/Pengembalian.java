package com.projekfajar.pengembalian.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Pengajuan pengembalian barang oleh pembeli beserta statusnya.
 *
 * Disetujui berarti uang (jumlah_refund) sudah dikembalikan dan dijurnal;
 * diterima berarti barang sudah sampai kembali dan stok dipulihkan.
 */
@Entity
@Table(name = "pengembalian")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pengembalian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nomor_pengembalian", nullable = false, length = 50, unique = true)
    private String nomorPengembalian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pesanan_id", nullable = false)
    private Pesanan pesanan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 1000)
    private String alasan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PengembalianStatus status = PengembalianStatus.DIAJUKAN;

    /** Nominal yang dikembalikan ke pembeli — default total yang dibayar. */
    @Column(name = "jumlah_refund", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal jumlahRefund = BigDecimal.ZERO;

    @Column(name = "catatan_admin", length = 1000)
    private String catatanAdmin;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "diterima_at")
    private LocalDateTime diterimaAt;
}
