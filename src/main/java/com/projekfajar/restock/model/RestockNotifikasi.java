package com.projekfajar.restock.model;

import java.time.LocalDateTime;

import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukVarian;
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

/** Daftar tunggu "beri tahu saya" ketika produk/varian kembali tersedia. */
@Entity
@Table(name = "restock_notifikasi")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockNotifikasi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produk_id", nullable = false)
    private Produk produk;

    /** Bila null berarti user menunggu stok produk dasar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProdukVarian varian;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime dikirimAt;
}
