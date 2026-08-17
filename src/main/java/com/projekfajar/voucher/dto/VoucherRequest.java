package com.projekfajar.voucher.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
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
public class VoucherRequest {

    @NotBlank(message = "Kode voucher tidak boleh kosong")
    private String kode;

    @NotBlank(message = "Jenis voucher tidak boleh kosong")
    private String jenis;

    @NotNull(message = "Nilai voucher tidak boleh kosong")
    @DecimalMin(value = "0.01", message = "Nilai voucher harus lebih dari 0")
    private BigDecimal nilai;

    private BigDecimal minBelanja;

    private BigDecimal maksDiskon;

    private Integer kuota;

    private Boolean aktif;

    private LocalDateTime berlakuDari;

    private LocalDateTime berlakuSampai;
}
