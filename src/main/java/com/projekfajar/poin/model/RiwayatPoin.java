package com.projekfajar.poin.model;

import java.time.LocalDateTime;

import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.user.model.User;
import com.projekfajar.voucher.model.Voucher;

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

/** Satu catatan perolehan atau pemakaian poin. */
@Entity
@Table(name = "riwayat_poin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiwayatPoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pesanan_id")
    private Pesanan pesanan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    /** Positif = perolehan, negatif = pemakaian. */
    @Column(nullable = false)
    private Long jumlah;

    @Column(length = 255)
    private String keterangan;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
