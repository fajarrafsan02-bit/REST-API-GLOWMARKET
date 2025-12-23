package com.projekfajar.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    @NotBlank(message = "Kode OTP tidak boleh kosong")
    @Pattern(regexp = "^[0-9]{4}$", message = "Kode OTP harus 4 digit angka")
    private String kode;
}
