package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nilai persediaan tidak diminta dari admin — sistem menghitungnya sendiri dari
 * stok × harga modal, supaya angka di neraca selalu cocok dengan katalog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaldoAwalRequest {
    private BigDecimal kas;
    private LocalDate tanggal;
}
