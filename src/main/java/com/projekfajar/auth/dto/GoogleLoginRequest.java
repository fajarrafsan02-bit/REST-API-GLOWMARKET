package com.projekfajar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {
    /** ID token JWT dari Google Identity Services (bukan access token). */
    @NotBlank(message = "Token Google tidak boleh kosong")
    private String credential;
}
