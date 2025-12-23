package com.projekfajar.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "produk_terjual")
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
    
    @Column(nullable = false)
    private Double hargaSaatBeli;
    
    @Column(nullable = false)
    private Double total;
    
    @Column(nullable = false)
    private LocalDateTime tanggalBeli;
    
    @ManyToOne
    @JoinColumn(name = "pesanan_id")
    private Pesanan pesanan;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSuccess = true; // true jika transaksi berhasil, false jika dibatalkan
}
