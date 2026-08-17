package com.projekfajar.payment.dto;

import java.math.BigDecimal;
import com.projekfajar.alamat.dto.AlamatResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String externalId;
    private String invoiceId;
    private String invoiceUrl;
    private BigDecimal amount;
    private BigDecimal ongkir;
    private BigDecimal diskon;
    private String kodeVoucher;
    private String ongkirSumber;
    private String ongkirKurir;
    private String ongkirLayanan;
    private String status;
    private String customerName;
    private String customerEmail;
    private AlamatResponse alamat;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
}
