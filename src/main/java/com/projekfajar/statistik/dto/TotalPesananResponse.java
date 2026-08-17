package com.projekfajar.statistik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalPesananResponse {
    private Long totalPesanan;
    private Integer bulan;
    private Integer tahun;
    private Double persenPesanan;
}
