package com.projekfajar.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerjualProdukResponse {
    private Long id;
    private Long produkId;
    private String namaProduk;
    private Double harga;
    private Integer terjual;
    private Integer karatEmas;
}
