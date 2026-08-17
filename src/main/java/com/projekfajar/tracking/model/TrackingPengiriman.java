package com.projekfajar.tracking.model;

import java.time.LocalDateTime;

import com.projekfajar.pesanan.model.Pesanan;

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

/** Satu peristiwa dalam perjalanan paket (satu baris timeline). */
@Entity
@Table(name = "tracking_pengiriman")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingPengiriman {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pesanan_id", nullable = false)
    private Pesanan pesanan;

    @Column(name = "nomor_resi", length = 100)
    private String nomorResi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrackingStatus status;

    @Column(length = 500)
    private String keterangan;

    @Column(length = 200)
    private String lokasi;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
