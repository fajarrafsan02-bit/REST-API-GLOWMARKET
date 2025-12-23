package com.projekfajar.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistikPenjualanResponse {
    private Double totalPenjualan;
    private Integer bulan;
    private Integer tahun;
    private Double persenPenjualan; // % perubahan vs bulan lalu
}
