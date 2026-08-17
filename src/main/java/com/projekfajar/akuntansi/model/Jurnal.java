package com.projekfajar.akuntansi.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Satu transaksi pembukuan, terdiri atas beberapa baris debit dan kredit.
 *
 * Jurnal adalah satu-satunya sumber kebenaran: buku besar, laba rugi, dan neraca
 * semuanya diturunkan dari sini, tidak ada saldo yang disimpan terpisah.
 */
@Entity
@Table(name = "jurnal", indexes = {
        @Index(name = "idx_jurnal_tanggal", columnList = "tanggal"),
        @Index(name = "idx_jurnal_sumber", columnList = "sumber, referensi_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jurnal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nomor;

    @Column(nullable = false)
    private LocalDate tanggal;

    @Column(nullable = false, length = 255)
    private String keterangan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SumberJurnal sumber;

    /** Id transaksi asal (pesanan, pembelian, beban) untuk penelusuran balik. */
    @Column(name = "referensi_id")
    private Long referensiId;

    @OneToMany(mappedBy = "jurnal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JurnalDetail> details = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public BigDecimal totalDebit() {
        return details.stream()
                .map(JurnalDetail::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalKredit() {
        return details.stream()
                .map(JurnalDetail::getKredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean seimbang() {
        return totalDebit().compareTo(totalKredit()) == 0;
    }
}
