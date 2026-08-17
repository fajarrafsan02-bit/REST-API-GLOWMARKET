package com.projekfajar.akuntansi.model;

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

/**
 * Satu baris pada bagan akun (chart of accounts).
 *
 * Kode akun dipakai langsung oleh pencatatan otomatis, jadi nilainya di-seed
 * lewat migrasi agar identik di semua lingkungan.
 */
@Entity
@Table(name = "akun")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Akun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String kode;

    @Column(nullable = false, length = 100)
    private String nama;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipeAkun tipe;

    /** DEBIT atau KREDIT — sisi yang menambah saldo akun ini. */
    @Column(name = "saldo_normal", nullable = false, length = 10)
    private String saldoNormal;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aktif = true;
}
