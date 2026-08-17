package com.projekfajar.pesanan.dto;

import java.math.BigDecimal;
import com.projekfajar.alamat.dto.AlamatResponse;
import com.projekfajar.payment.dto.PaymentResponse;
import com.projekfajar.pesanan.model.OrderStatus;
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
    /** External ID invoice — dipakai frontend untuk tombol "Bayar Sekarang" pada pesanan PENDING. */
    private String externalId;
    private Long userId;
    private String userName;
    private BigDecimal totalHarga;
    private BigDecimal ongkir;
    private BigDecimal diskon;
    private String kodeVoucher;
    private String ongkirSumber;
    private String ongkirKurir;
    private String ongkirLayanan;
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
