package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Keadaan saldo awal untuk halaman admin: sudah dicatat atau belum, dan taksiran nilainya. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaldoAwalInfo {
    private boolean sudahDicatat;
    private BigDecimal nilaiPersediaan;
    private long produkTanpaModal;
    private long totalProduk;
}
