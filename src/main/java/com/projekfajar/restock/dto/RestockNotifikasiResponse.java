package com.projekfajar.restock.dto;

import java.time.LocalDateTime;

import com.projekfajar.restock.model.RestockNotifikasi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockNotifikasiResponse {
    private Long id;
    private Long produkId;
    private String namaProduk;
    private Long variantId;
    private String namaVariant;
    private Boolean aktif;
    private LocalDateTime createdAt;
    private LocalDateTime dikirimAt;

    public static RestockNotifikasiResponse from(RestockNotifikasi n) {
        return RestockNotifikasiResponse.builder()
                .id(n.getId())
                .produkId(n.getProduk().getId())
                .namaProduk(n.getProduk().getNama())
                .variantId(n.getVarian() != null ? n.getVarian().getId() : null)
                .namaVariant(n.getVarian() != null ? n.getVarian().getNama() : null)
                .aktif(n.getAktif())
                .createdAt(n.getCreatedAt())
                .dikirimAt(n.getDikirimAt())
                .build();
    }
}
