package com.projekfajar.voucher.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projekfajar.voucher.model.Voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private Long id;
    private String kode;
    private String jenis;
    private BigDecimal nilai;
    private BigDecimal minBelanja;
    private BigDecimal maksDiskon;
    private Integer kuota;
    private Integer terpakai;
    private Boolean aktif;
    private LocalDateTime berlakuDari;
    private LocalDateTime berlakuSampai;
    private LocalDateTime createdAt;

    public static VoucherResponse from(Voucher v) {
        return VoucherResponse.builder()
                .id(v.getId())
                .kode(v.getKode())
                .jenis(v.getJenis())
                .nilai(v.getNilai())
                .minBelanja(v.getMinBelanja())
                .maksDiskon(v.getMaksDiskon())
                .kuota(v.getKuota())
                .terpakai(v.getTerpakai())
                .aktif(v.getAktif())
                .berlakuDari(v.getBerlakuDari())
                .berlakuSampai(v.getBerlakuSampai())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
