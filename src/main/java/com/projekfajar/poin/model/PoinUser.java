package com.projekfajar.poin.model;

import java.time.LocalDateTime;

import com.projekfajar.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Saldo poin loyalitas milik satu user. */
@Entity
@Table(name = "poin_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoinUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "saldo_poin", nullable = false)
    @Builder.Default
    private Long saldoPoin = 0L;

    @Column(name = "total_diperoleh", nullable = false)
    @Builder.Default
    private Long totalDiperoleh = 0L;

    @Column(name = "total_dipakai", nullable = false)
    @Builder.Default
    private Long totalDipakai = 0L;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
