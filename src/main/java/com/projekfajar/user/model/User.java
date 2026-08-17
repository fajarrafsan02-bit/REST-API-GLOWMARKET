package com.projekfajar.user.model;

import com.projekfajar.auth.model.Role;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String namaLengkap;
    private String email;
    private String password;
    private String noHp;

    /**
     * Subjek (sub) token Google untuk user yang login/daftar lewat akun
     * Google. Null bila akun didaftarkan manual dan belum pernah dipakai
     * login Google.
     */
    @Column(unique = true)
    private String googleId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    /**
     * Status aktif/nonaktif akun yang dikendalikan admin (bukan verifikasi email).
     * Dicek saat login: akun nonaktif ditolak masuk.
     */
    @Builder.Default
    private Boolean terverifikasi = true;

    /**
     * Bukti bahwa alamat email benar-benar dimiliki pengguna.
     * Wajib true sebelum bisa checkout, agar bukti pembayaran dan pemberitahuan
     * status pesanan dipastikan sampai.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailTerverifikasi = false;

    private LocalDateTime emailTerverifikasiAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime lastLogin;
}
