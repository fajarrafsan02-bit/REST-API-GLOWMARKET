package com.projekfajar.keranjang.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeranjangRequest {
    @NotNull(message = "Produk ID tidak boleh kosong")
    private Long produkId;

    /** Wajib bila produk punya varian; null memakai harga/stock dasar produk. */
    private Long variantId;

    @NotNull(message = "Quantity tidak boleh kosong")
    @Min(value = 1, message = "Quantity minimal 1")
    private Integer quantity;
}
