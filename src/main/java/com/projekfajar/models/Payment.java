package com.projekfajar.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String externalId;
    
    @Column(nullable = false)
    private String xenditInvoiceId;
    
    @Column(nullable = false)
    private String invoiceUrl;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String status; // PENDING, PAID, EXPIRED, FAILED
    
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String customerEmail;
    
    private String customerPhone;
    
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "alamat_id")
    private Alamat alamat;
    
    private String catatan;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    @Column(nullable = false)
    private LocalDateTime expiredAt;
    
    private LocalDateTime updatedAt;
}
