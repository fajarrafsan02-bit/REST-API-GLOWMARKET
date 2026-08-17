package com.projekfajar.pesanan.model;

import java.math.BigDecimal;
import com.projekfajar.produk.model.Produk;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pesanan_item", indexes = {
        @Index(name = "idx_pesanan_item_pesanan", columnList = "pesanan_id"),
        @Index(name = "idx_pesanan_item_produk", columnList = "produk_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PesananItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "pesanan_id", nullable = false)
    private Pesanan pesanan;
    
    @ManyToOne
    @JoinColumn(name = "produk_id", nullable = false)
    private Produk produk;

    /** Varian yang dipilih; null berarti memakai harga/stock dasar produk. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private com.projekfajar.produk.model.ProdukVarian variant;

    /** Salinan nama varian, agar riwayat tidak berubah bila varian diubah admin. */
    @Column(name = "nama_variant", length = 100)
    private String namaVariant;

    /** Salinan identitas produk saat dibeli, agar riwayat tidak ikut berubah
     *  ketika nama atau gambar produk diperbarui admin. */
    private String namaProduk;

    @Column(columnDefinition = "TEXT")
    private String gambarProduk;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal hargaSatuan;

    /** Salinan harga modal saat barang terjual — dasar HPP pesanan ini. */
    @Column(name = "harga_modal", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal hargaModal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    
    private Integer karatEmas;
}
