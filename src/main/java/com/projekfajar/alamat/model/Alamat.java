package com.projekfajar.alamat.model;

import com.projekfajar.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alamat")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alamat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String namaLengkap;
    
    @Column(nullable = false)
    private String nomorTelepon;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String alamatLengkap;
    
    private String provinsi;
    
    private String kota;
    
    private String kecamatan;
    
    private String kelurahan;
    
    @Column(nullable = false)
    private String kodePos;

    /**
     * ID kecamatan milik RajaOngkir untuk alamat ini — diisi lazy (baru
     * dicari saat pertama kali dipakai menghitung ongkir), bukan saat alamat
     * disimpan, supaya alamat yang tidak pernah dipakai belanja tidak ikut
     * memakan kuota harian API pencarian.
     */
    private String rajaongkirDestinationId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    private String catatan;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
}
