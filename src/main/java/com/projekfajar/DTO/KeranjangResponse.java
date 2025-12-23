package com.projekfajar.DTO;

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
    private Integer quantity;
    private Double subtotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
