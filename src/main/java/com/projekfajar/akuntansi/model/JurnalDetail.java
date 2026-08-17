package com.projekfajar.akuntansi.model;

import java.math.BigDecimal;

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

/** Satu baris debit atau kredit di dalam sebuah jurnal. */
@Entity
@Table(name = "jurnal_detail", indexes = {
        @Index(name = "idx_jurnal_detail_jurnal", columnList = "jurnal_id"),
        @Index(name = "idx_jurnal_detail_akun", columnList = "akun_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JurnalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "jurnal_id", nullable = false)
    private Jurnal jurnal;

    @ManyToOne
    @JoinColumn(name = "akun_id", nullable = false)
    private Akun akun;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal kredit = BigDecimal.ZERO;

    @Column(length = 255)
    private String keterangan;
}
