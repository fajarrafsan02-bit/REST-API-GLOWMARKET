package com.projekfajar.review.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {
    
    private Long id;
    private Long userId;
    private String userName;
    private Long produkId;
    private String namaProduk;
    private Long pesananId;
    private String nomorPesanan;
    private Integer rating;
    private String komentar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
