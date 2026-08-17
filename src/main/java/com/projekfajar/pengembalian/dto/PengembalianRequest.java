package com.projekfajar.pengembalian.dto;

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
public class PengembalianRequest {

    @NotNull(message = "Pesanan wajib dipilih")
    private Long pesananId;

    @NotBlank(message = "Alasan pengembalian wajib diisi")
    private String alasan;
}
