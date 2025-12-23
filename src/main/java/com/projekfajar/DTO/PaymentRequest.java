package com.projekfajar.DTO;

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
public class PaymentRequest {

    @NotBlank(message = "Nama customer tidak boleh kosong")
    private String customerName;

    @NotBlank(message = "Email customer tidak boleh kosong")
    private String customerEmail;

    private String customerPhone;

    @NotNull(message = "Total amount tidak boleh kosong")
    // @Min(value = 10000, message = "Minimum pembayaran Rp 10.000")
    private Double amount;

    private String description;

    // Optional: untuk payment dari keranjang
    private Long userId;

    // Shipping details
    private Long alamatId;
    private String catatan;
}
