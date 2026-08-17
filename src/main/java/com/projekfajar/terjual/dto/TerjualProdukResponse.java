package com.projekfajar.terjual.dto;

import java.math.BigDecimal;
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
    private String gambar;
    private BigDecimal harga;
    private Integer terjual;
    private Integer karatEmas;
}
