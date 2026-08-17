package com.projekfajar.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Verifikasi ID token "Sign in with Google" dilakukan di server memakai
 * pustaka resmi Google (menghubungi Google untuk memvalidasi tanda tangan
 * token) — jangan pernah mempercayai data profil yang dikirim langsung dari
 * browser, karena itu bisa dipalsukan.
 */
@Configuration
public class GoogleAuthConfig {

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }
}
