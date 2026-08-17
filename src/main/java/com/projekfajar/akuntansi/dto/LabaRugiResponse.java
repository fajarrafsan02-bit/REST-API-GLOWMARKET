package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Laba rugi periode berjalan.
 *
 * Semua angka diturunkan dari jurnal, jadi laporan ini tidak pernah bisa
 * berbeda dari mutasi yang sebenarnya tercatat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabaRugiResponse {

    private LocalDate mulai;
    private LocalDate sampai;

    private List<BarisAkun> pendapatan;
    private BigDecimal totalPendapatan;

    private List<BarisAkun> hpp;
    private BigDecimal totalHpp;

    /** Pendapatan − HPP. */
    private BigDecimal labaKotor;

    private List<BarisAkun> beban;
    private BigDecimal totalBeban;

    /** Laba kotor − beban operasional. */
    private BigDecimal labaBersih;

    /** Jumlah pesanan lunas yang HPP-nya tidak tercatat karena harga modalnya nol. */
    private long penjualanTanpaHpp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarisAkun {
        private String kode;
        private String nama;
        private BigDecimal jumlah;
    }
}
