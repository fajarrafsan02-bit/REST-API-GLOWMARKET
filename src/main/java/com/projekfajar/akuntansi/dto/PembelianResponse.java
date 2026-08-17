package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.projekfajar.akuntansi.model.MetodePembelian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PembelianResponse {

    private Long id;
    private String nomor;
    private LocalDate tanggal;
    private String pemasok;
    private BigDecimal total;
    private MetodePembelian metode;
    private boolean dilunasi;
    private LocalDateTime dilunasiAt;
    private String catatan;
    private boolean dibatalkan;
    private LocalDateTime createdAt;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long produkId;
        private String namaProduk;
        private Integer qty;
        private BigDecimal hargaBeli;
        private BigDecimal subtotal;
    }
}
