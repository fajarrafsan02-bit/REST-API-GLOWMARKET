package com.projekfajar.DTO;

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
    private Double penjualan;
    private Long produkTerjual;
}
