package com.projekfajar.DTO;

import com.projekfajar.models.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PesananResponse {
    private Long id;
    private String nomorPesanan;
    private Long userId;
    private String userName;
    private Double totalHarga;
    private OrderStatus status;
    private AlamatResponse alamat;
    private String catatan;
    private String nomorResi;
    private LocalDateTime createdAt;
    private LocalDateTime dikemasAt;
    private LocalDateTime dikirimAt;
    private LocalDateTime selesaiAt;
    private List<PesananItemResponse> items;
    private PaymentResponse payment;
}
