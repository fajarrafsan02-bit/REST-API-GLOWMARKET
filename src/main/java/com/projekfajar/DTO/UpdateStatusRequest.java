package com.projekfajar.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    
    @NotBlank(message = "Status tidak boleh kosong")
    private String status; // DIKEMAS, DIKIRIM, SELESAI, DIBATALKAN
    
    private String nomorResi; // Wajib untuk status DIKIRIM
}
