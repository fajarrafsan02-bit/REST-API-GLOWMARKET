package com.projekfajar.keranjang.model;

import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukVarian;
import com.projekfajar.user.model.User;
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
 * Unik per (user, produk, varian) — dijamin index di migrasi V15 karena
 * produk tanpa varian (variant_id NULL) juga harus hanya muncul satu baris.
 */
@Entity
@Table(name = "keranjang")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Keranjang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "produk_id", nullable = false)
    private Produk produk;

    /** Pilihan varian; null berarti memakai harga/stock dasar produk. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProdukVarian variant;

    @Column(nullable = false)
    private Integer quantity;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
