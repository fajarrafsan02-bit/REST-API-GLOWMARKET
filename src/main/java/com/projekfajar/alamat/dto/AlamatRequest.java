package com.projekfajar.alamat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlamatRequest {
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
}
