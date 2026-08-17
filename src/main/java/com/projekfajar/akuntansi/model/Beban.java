package com.projekfajar.akuntansi.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Biaya operasional: gaji, sewa, listrik, transport, dan sejenisnya.
 *
 * Akunnya dipilih admin dari bagan akun bertipe BEBAN, sehingga laporan laba rugi
 * bisa merinci pengeluaran per pos tanpa perlu kategori terpisah.
 */
@Entity
@Table(name = "beban", indexes = {
        @Index(name = "idx_beban_tanggal", columnList = "tanggal"),
        @Index(name = "idx_beban_akun", columnList = "akun_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beban {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate tanggal;

    @ManyToOne
    @JoinColumn(name = "akun_id", nullable = false)
    private Akun akun;

    @Column(nullable = false, length = 255)
    private String keterangan;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal jumlah;

    @Column(nullable = false)
    @Builder.Default
    private Boolean dibatalkan = false;

    @Column(name = "dibatalkan_at")
    private LocalDateTime dibatalkanAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
