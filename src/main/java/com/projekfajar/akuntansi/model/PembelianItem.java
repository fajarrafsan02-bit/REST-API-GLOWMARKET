package com.projekfajar.akuntansi.model;

import java.math.BigDecimal;

import com.projekfajar.produk.model.Produk;

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

@Entity
@Table(name = "pembelian_item", indexes = {
        @Index(name = "idx_pembelian_item_pembelian", columnList = "pembelian_id"),
        @Index(name = "idx_pembelian_item_produk", columnList = "produk_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PembelianItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pembelian_id", nullable = false)
    private Pembelian pembelian;

    @ManyToOne
    @JoinColumn(name = "produk_id", nullable = false)
    private Produk produk;

    /** Salinan nama produk saat dibeli, sama alasannya dengan item pesanan. */
    @Column(name = "nama_produk")
    private String namaProduk;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "harga_beli", nullable = false, precision = 19, scale = 2)
    private BigDecimal hargaBeli;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
}
