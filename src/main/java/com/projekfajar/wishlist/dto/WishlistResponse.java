package com.projekfajar.wishlist.dto;

import com.projekfajar.produk.dto.ProdukResponse;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private Long id;
    private Long userId;
    private Long produkId;
    private ProdukResponse produk;
    private LocalDateTime createdAt;
}
