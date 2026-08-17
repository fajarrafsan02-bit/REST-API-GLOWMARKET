package com.projekfajar.statistik.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaporanHarianResponse {
    private LocalDate tanggal;
    private Long pesanan;
    private BigDecimal penjualan;
    private Long produkTerjual;
}
