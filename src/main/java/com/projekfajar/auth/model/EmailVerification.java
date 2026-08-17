package com.projekfajar.auth.model;

import java.time.LocalDateTime;

import com.projekfajar.user.model.User;

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

/**
 * Kode verifikasi kepemilikan email saat registrasi.
 *
 * Sengaja terpisah dari {@link LoginOtp} yang dipakai untuk login admin: masa
 * berlakunya berbeda, pemakainya berbeda, dan mencampur keduanya membuat satu
 * alur bisa membatalkan kode milik alur lain.
 */
@Entity
@Table(name = "email_verification", indexes = {
        @Index(name = "idx_email_verification_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 6)
    private String kode;

    /** Jumlah percobaan salah; kode dibatalkan setelah batas tercapai. */
    @Column(nullable = false)
    @Builder.Default
    private Integer percobaan = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean kedaluwarsa() {
        return expiredAt.isBefore(LocalDateTime.now());
    }
}
