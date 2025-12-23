package com.projekfajar.DTO;

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
    private Double totalPenjualan; // Total Rp penjualan
}
