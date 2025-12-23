package com.projekfajar.DTO;

import java.time.LocalDateTime;

import com.projekfajar.models.StatusProduk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProdukResponse {
    private Long id;
    private String nama;
    private String gambar;
    private Double harga;
    private Integer stock;
    private Integer karatEmas;
    private Double beratGram;
    private StatusProduk status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
