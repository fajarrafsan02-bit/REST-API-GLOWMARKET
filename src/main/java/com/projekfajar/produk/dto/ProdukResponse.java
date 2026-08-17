package com.projekfajar.produk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.projekfajar.produk.model.StatusProduk;

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
    private String deskripsi;
    private String gambar;
    /** Foto produk berurutan; item pertama sama dengan {@code gambar}. */
    private List<String> gambarList;
    private String kategori;
    private BigDecimal harga;

    /** Harga beli — dipakai menghitung HPP dan laba. */
    private BigDecimal hargaModal;
    private Integer stock;
    /** Jumlah unit terjual, dihitung dari catatan transaksi. */
    private Integer terjual;
    private Integer karatEmas;
    private Double beratGram;
    private StatusProduk status;

    /** Pilihan barang (ukuran/gramasi). Kosong bila produk tidak punya varian. */
    private List<ProdukVarianResponse> varian;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
