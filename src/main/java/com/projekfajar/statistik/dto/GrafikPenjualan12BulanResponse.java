package com.projekfajar.statistik.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrafikPenjualan12BulanResponse {
    private Integer bulan;
    private Integer tahun;
    private String namaBulan;
    private Long totalProdukTerjual; // Jumlah produk terjual (distinct produk)
    private BigDecimal totalPenjualan; // Total Rp penjualan
}
