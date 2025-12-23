package com.projekfajar.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pesanan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pesanan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nomorPesanan;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @OneToMany(mappedBy = "pesanan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PesananItem> items = new ArrayList<>();
    
    @Column(nullable = false)
    private Double totalHarga;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    @ManyToOne
    @JoinColumn(name = "alamat_id")
    private Alamat alamat;
    
    private String catatan;
    
    private String nomorResi;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime dikemasAt;
    
    private LocalDateTime dikirimAt;
    
    private LocalDateTime selesaiAt;
    
    private LocalDateTime updatedAt;
}
