package com.projekfajar.akuntansi.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pembelian stok dari pemasok: menambah persediaan, dan mengurangi kas (tunai)
 * atau menambah utang usaha (kredit) sampai dilunasi.
 *
 * Pembelian tidak pernah dihapus, hanya ditandai dibatalkan, karena jurnalnya
 * menunjuk ke baris ini lewat referensi_id.
 */
@Entity
@Table(name = "pembelian", indexes = {
        @Index(name = "idx_pembelian_tanggal", columnList = "tanggal")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pembelian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nomor;

    @Column(nullable = false)
    private LocalDate tanggal;

    @Column(length = 150)
    private String pemasok;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MetodePembelian metode = MetodePembelian.TUNAI;

    @Column(nullable = false)
    @Builder.Default
    private Boolean dilunasi = true;

    @Column(name = "dilunasi_at")
    private LocalDateTime dilunasiAt;

    @Column(length = 255)
    private String catatan;

    @Column(nullable = false)
    @Builder.Default
    private Boolean dibatalkan = false;

    @Column(name = "dibatalkan_at")
    private LocalDateTime dibatalkanAt;

    @OneToMany(mappedBy = "pembelian", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PembelianItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
