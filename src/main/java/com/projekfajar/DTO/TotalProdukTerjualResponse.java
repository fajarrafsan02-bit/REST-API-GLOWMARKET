package com.projekfajar.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalProdukTerjualResponse {
    private Long totalJenisProduk;
    private Integer bulan;
    private Integer tahun;
    private Double persenProduk;
}
