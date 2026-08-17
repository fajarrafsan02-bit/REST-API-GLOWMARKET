package com.projekfajar.terjual.model;

import java.math.BigDecimal;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "produk_terjual", indexes = {
        @Index(name = "idx_produk_terjual_produk", columnList = "produk_id"),
        @Index(name = "idx_produk_terjual_tanggal", columnList = "tanggalBeli"),
        @Index(name = "idx_produk_terjual_pesanan", columnList = "pesanan_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdukTerjual {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "produk_id", nullable = false)
    private Produk produk;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Integer qty;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal hargaSaatBeli;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;
    
    @Column(nullable = false)
    private LocalDateTime tanggalBeli;
    
    @ManyToOne
    @JoinColumn(name = "pesanan_id")
    private Pesanan pesanan;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSuccess = true; // true jika transaksi berhasil, false jika dibatalkan
}
