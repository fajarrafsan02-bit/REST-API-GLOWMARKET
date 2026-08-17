package com.projekfajar.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {
    
    @NotNull(message = "Produk ID tidak boleh kosong")
    private Long produkId;
    
    @NotNull(message = "Pesanan ID tidak boleh kosong")
    private Long pesananId;
    
    @NotNull(message = "Rating tidak boleh kosong")
    @Min(value = 1, message = "Rating minimal 1")
    @Max(value = 5, message = "Rating maksimal 5")
    private Integer rating;
    
    @NotBlank(message = "Komentar tidak boleh kosong")
    private String komentar;
}
