package com.projekfajar.produk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
 * Satu pilihan dari sebuah produk — misalnya ukuran cincin atau gramasi.
 *
 * Setiap varian punya harga, harga modal, dan stok sendiri. Produk tanpa
 * varian tetap berjalan seperti sebelumnya (harga/stock di tabel produk).
 */
@Entity
@Table(name = "produk_variant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdukVarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produk_id", nullable = false)
    private Produk produk;

    @Column(nullable = false, length = 100)
    private String nama;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal harga;

    @Column(name = "harga_modal", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal hargaModal = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
