package com.projekfajar.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilRequest {
    private String namaLengkap;
    private String noHp;
    private String passwordLama;
    private String passwordBaru;
}
