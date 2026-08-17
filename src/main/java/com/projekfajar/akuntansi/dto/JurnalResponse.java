package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Satu jurnal beserta baris-barisnya, untuk halaman Jurnal Umum. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JurnalResponse {

    private Long id;
    private String nomor;
    private LocalDate tanggal;
    private String keterangan;
    private String sumber;
    private Long referensiId;
    private BigDecimal totalDebit;
    private BigDecimal totalKredit;
    private List<Baris> baris;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Baris {
        private String kodeAkun;
        private String namaAkun;
        private BigDecimal debit;
        private BigDecimal kredit;
        private String keterangan;
    }
}
