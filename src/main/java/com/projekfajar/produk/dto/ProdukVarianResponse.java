package com.projekfajar.produk.dto;

import java.math.BigDecimal;

import com.projekfajar.produk.model.ProdukVarian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdukVarianResponse {
    private Long id;
    private Long produkId;
    private String nama;
    private BigDecimal harga;
    private BigDecimal hargaModal;
    private Integer stock;
    private Boolean aktif;

    public static ProdukVarianResponse from(ProdukVarian v) {
        return from(v, true);
    }

    /**
     * Harga modal adalah rahasia dagang — nilainya hanya diisi bila yang
     * meminta seorang admin (sama seperti hargaModal pada ProdukResponse).
     */
    public static ProdukVarianResponse from(ProdukVarian v, boolean includeModal) {
        return ProdukVarianResponse.builder()
                .id(v.getId())
                .produkId(v.getProduk().getId())
                .nama(v.getNama())
                .harga(v.getHarga())
                .hargaModal(includeModal ? v.getHargaModal() : null)
                .stock(v.getStock())
                .aktif(v.getAktif())
                .build();
    }
}
