package com.projekfajar.pesanan.dto;

import java.math.BigDecimal;
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
    private Long variantId;
    private String namaVariant;
    private Integer quantity;
    private BigDecimal hargaSatuan;
    private BigDecimal subtotal;
    private Integer karatEmas;
}
