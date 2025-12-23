package com.projekfajar.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrafikPenjualanBulananResponse {
    private Integer bulan;
    private Integer tahun;
    private String namaBulan;
    private Long totalProdukTerjual;
    private Long totalPesanan;
    private Double totalPenjualan;
}
