package com.projekfajar.restock.dto;

import com.projekfajar.restock.model.RestockNotifikasi;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockNotifikasiRequest {

    @NotNull(message = "Produk wajib dipilih")
    private Long produkId;

    /** Kosong berarti menunggu stok produk dasar. */
    private Long variantId;
}
