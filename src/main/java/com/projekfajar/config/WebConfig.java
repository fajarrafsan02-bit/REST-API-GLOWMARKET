package com.projekfajar.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sebelumnya origin di sini di-hardcode terpisah dari SecurityConfig, jadi
 * dua konfigurasi CORS bisa diam-diam berbeda. Sekarang keduanya membaca
 * properti yang sama — penting sejak autentikasi memakai cookie ber-kredensial,
 * karena origin yang salah/ketinggalan zaman di sini adalah lubang keamanan,
 * bukan cuma bug fungsional.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
