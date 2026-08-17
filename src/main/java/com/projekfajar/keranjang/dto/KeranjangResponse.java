package com.projekfajar.keranjang.dto;

import java.math.BigDecimal;
import com.projekfajar.produk.dto.ProdukResponse;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeranjangResponse {
    private Long id;
    private Long userId;
    private ProdukResponse produk;
    private Long variantId;
    private String namaVariant;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
