package com.projekfajar.user.dto;

import com.projekfajar.auth.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String namaLengkap;
    private String email;
    private String noHp;
    private Role role;
    /** Status aktif/nonaktif akun yang dikendalikan admin. */
    private Boolean terverifikasi;

    /** Bukti kepemilikan email — syarat untuk bisa checkout. */
    private Boolean emailTerverifikasi;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
