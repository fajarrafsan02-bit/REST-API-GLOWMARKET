package com.projekfajar.DTO;

import com.projekfajar.models.StatusProduk;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProdukRequest {
    @NotBlank(message = "Nama produk tidak boleh kosong")
    private String nama;

    private String gambar;

    @NotNull(message = "Harga tidak boleh kosong")
    @Min(value = 0, message = "Harga tidak boleh negatif")
    private Double harga;

    @NotNull(message = "Stock tidak boleh kosong")
    @Min(value = 0, message = "Stock tidak boleh negatif")
    private Integer stock;

    @NotNull(message = "Karat emas tidak boleh kosong")
    @Min(value = 1, message = "Karat emas tidak valid")
    private Integer karatEmas;
    
    @NotNull(message = "Berat gram tidak boleh kosong")
    @Min(value = 0, message = "Berat gram tidak boleh negatif")
    private Double beratGram;

    private StatusProduk status;
}
