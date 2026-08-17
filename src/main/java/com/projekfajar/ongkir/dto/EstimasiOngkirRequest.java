package com.projekfajar.ongkir.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EstimasiOngkirRequest {
    @NotNull
    private Long alamatId;
}
