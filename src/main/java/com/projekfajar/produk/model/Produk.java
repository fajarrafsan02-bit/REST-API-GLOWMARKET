package com.projekfajar.produk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Produk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nama;

    /** Motif, ukuran, perawatan — opsional, produk lama tetap valid tanpa teks. */
    @Column(columnDefinition = "TEXT")
    private String deskripsi;

    @Column(columnDefinition = "TEXT")
    private String gambar; // URL or path to image

    /** Kategori produk (Cincin, Kalung, ...). Nullable agar produk lama tetap valid. */
    @Column(length = 50)
    private String kategori;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal harga;

    /**
     * Harga modal (harga beli) — dasar perhitungan HPP dan laba.
     * Nilainya disalin ke item pesanan saat checkout, sehingga mengubah angka ini
     * tidak akan mengubah laba pesanan yang sudah terjadi.
     */
    @Column(name = "harga_modal", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal hargaModal = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "karat_emas", nullable = false)
    private Integer karatEmas;
    
    @Column(name = "berat_gram", nullable = false)
    private Double beratGram;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusProduk status = StatusProduk.TERSEDIA;

    /**
     * Produk tidak pernah benar-benar dihapus: barisnya masih ditunjuk oleh
     * pesanan, review, dan catatan penjualan. Menghapusnya akan merusak
     * riwayat (atau langsung gagal karena foreign key).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
