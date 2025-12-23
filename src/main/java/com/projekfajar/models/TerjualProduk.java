package com.projekfajar.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "terjual_produk")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerjualProduk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "produk_id", nullable = false, unique = true)
    private Produk produk;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer terjual = 0;
}
