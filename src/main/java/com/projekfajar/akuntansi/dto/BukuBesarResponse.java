package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mutasi satu akun pada satu rentang tanggal, lengkap dengan saldo berjalan. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BukuBesarResponse {

    private String kodeAkun;
    private String namaAkun;
    private String saldoNormal;
    private LocalDate mulai;
    private LocalDate sampai;

    private BigDecimal saldoAwal;
    private BigDecimal totalDebit;
    private BigDecimal totalKredit;
    private BigDecimal saldoAkhir;

    private List<Mutasi> mutasi;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mutasi {
        private LocalDate tanggal;
        private String nomorJurnal;
        private String keterangan;
        private BigDecimal debit;
        private BigDecimal kredit;
        /** Saldo setelah baris ini, dalam arah saldo normal akunnya. */
        private BigDecimal saldo;
    }
}
