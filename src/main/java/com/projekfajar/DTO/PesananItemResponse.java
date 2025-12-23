package com.projekfajar.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PesananItemResponse {
    private Long id;
    private Long produkId;
    private String namaProduk;
    private String gambarProduk;
    private Integer quantity;
    private Double hargaSatuan;
    private Double subtotal;
    private Integer karatEmas;
}
