package com.projekfajar.produk.dto;

import java.math.BigDecimal;
import java.util.List;

import com.projekfajar.produk.model.StatusProduk;

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

    private String deskripsi;

    private String gambar;

    /** URL foto berurutan. Foto pertama menjadi gambar utama. Maksimal 8. */
    private List<String> gambarList;

    private String kategori;

    @NotNull(message = "Harga tidak boleh kosong")
    @Min(value = 0, message = "Harga tidak boleh negatif")
    private BigDecimal harga;

    @Min(value = 0, message = "Harga modal tidak boleh negatif")
    private BigDecimal hargaModal;

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
