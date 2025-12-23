package com.projekfajar.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pesanan_item")
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
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Double hargaSatuan;
    
    @Column(nullable = false)
    private Double subtotal;
    
    private Integer karatEmas;
}
