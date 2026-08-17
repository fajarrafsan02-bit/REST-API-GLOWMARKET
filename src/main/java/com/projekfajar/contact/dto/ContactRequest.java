package com.projekfajar.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactRequest {
    
    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    private String namaLengkap;
    
    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;
    
    @NotBlank(message = "Nomor telepon tidak boleh kosong")
    private String noTelepon;
    
    @NotBlank(message = "Pesan tidak boleh kosong")
    private String pesan;
}
