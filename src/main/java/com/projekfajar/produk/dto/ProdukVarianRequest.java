package com.projekfajar.produk.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdukVarianRequest {

    @NotBlank(message = "Nama varian tidak boleh kosong")
    private String nama;

    @NotNull(message = "Harga varian tidak boleh kosong")
    @DecimalMin(value = "0.00", message = "Harga varian tidak boleh negatif")
    private BigDecimal harga;

    @DecimalMin(value = "0.00", message = "Harga modal varian tidak boleh negatif")
    private BigDecimal hargaModal;

    @NotNull(message = "Stock varian tidak boleh kosong")
    @Min(value = 0, message = "Stock varian tidak boleh negatif")
    private Integer stock;

    private Boolean aktif;
}
