package com.projekfajar.poin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TukarPoinRequest {

    @NotNull(message = "Jumlah poin wajib diisi")
    @Min(value = 100, message = "Penukaran minimal 100 poin")
    @Max(value = 1000000, message = "Jumlah poin terlalu besar")
    private Long jumlahPoin;
}
