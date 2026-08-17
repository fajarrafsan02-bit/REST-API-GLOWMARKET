package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public class BebanRequest {

    private LocalDate tanggal;

    /** Kode akun beban (6-100, 6-200, ...) yang dipilih admin. */
    @NotBlank(message = "Akun beban tidak boleh kosong")
    private String kodeAkun;

    @NotBlank(message = "Keterangan tidak boleh kosong")
    private String keterangan;

    @NotNull(message = "Jumlah tidak boleh kosong")
    @DecimalMin(value = "0.01", message = "Jumlah harus lebih dari nol")
    private BigDecimal jumlah;
}
