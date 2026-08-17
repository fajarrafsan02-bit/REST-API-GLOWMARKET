package com.projekfajar.alamat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlamatResponse {
    private Long id;
    private Long userId;
    private String namaLengkap;
    private String nomorTelepon;
    private String alamatLengkap;
    private String provinsi;
    private String kota;
    private String kecamatan;
    private String kelurahan;
    private String kodePos;
    private Boolean isDefault;
    private String catatan;
    private LocalDateTime createdAt;
}
