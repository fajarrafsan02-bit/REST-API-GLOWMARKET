package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.projekfajar.akuntansi.model.MetodePembelian;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PembelianRequest {

    private LocalDate tanggal;

    private String pemasok;

    private String catatan;

    /** TUNAI mengurangi kas sekarang; KREDIT masuk Utang Usaha sampai dilunasi. Null = TUNAI. */
    private MetodePembelian metode;

    @NotEmpty(message = "Pembelian harus punya minimal satu barang")
    @Valid
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotNull(message = "Produk tidak boleh kosong")
        private Long produkId;

        @NotNull(message = "Jumlah tidak boleh kosong")
        @Min(value = 1, message = "Jumlah minimal 1")
        private Integer qty;

        @NotNull(message = "Harga beli tidak boleh kosong")
        @DecimalMin(value = "0.0", message = "Harga beli tidak boleh negatif")
        private BigDecimal hargaBeli;
    }
}
