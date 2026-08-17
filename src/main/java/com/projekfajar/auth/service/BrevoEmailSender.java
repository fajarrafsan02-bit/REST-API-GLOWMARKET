package com.projekfajar.auth.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.projekfajar.exception.EmailSendException;

import lombok.extern.slf4j.Slf4j;

/**
 * Pengiriman lewat HTTP API Brevo — dipakai di produksi.
 *
 * Render memblokir port SMTP keluar, jadi email harus berangkat lewat HTTPS
 * (port 443) yang tidak pernah diblokir. Isi pesannya tetap disusun
 * EmailService; kelas ini hanya mengurus pengangkutannya.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
@Slf4j
public class BrevoEmailSender implements EmailSender {

    private final WebClient webClient;
    private final String pengirimEmail;
    private final String pengirimNama;

    public BrevoEmailSender(
            @Value("${app.email.brevo.api-key:}") String apiKey,
            @Value("${app.email.from:}") String pengirimEmail,
            @Value("${app.email.from-name:GlowMarket}") String pengirimNama) {

        this.pengirimEmail = pengirimEmail;
        this.pengirimNama = pengirimNama;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void kirim(String tujuan, String subjek, String htmlBody, String replyTo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("email", pengirimEmail, "name", pengirimNama));
        body.put("to", List.of(Map.of("email", tujuan)));
        body.put("subject", subjek);
        body.put("htmlContent", htmlBody);
        if (replyTo != null && !replyTo.isBlank()) {
            body.put("replyTo", Map.of("email", replyTo));
        }

        try {
            webClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    // Batas waktu tegas: pengiriman OTP berjalan sinkron di
                    // dalam alur login, jadi kegagalan harus cepat diketahui
                    // daripada menggantung menunggu balasan.
                    .block(Duration.ofSeconds(20));

            log.info("Email '{}' terkirim ke {} lewat Brevo", subjek, tujuan);
        } catch (WebClientResponseException e) {
            log.error("Brevo menolak pengiriman ke {} (HTTP {}): {}",
                    tujuan, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new EmailSendException("Gagal mengirim email ke " + tujuan, e);
        } catch (Exception e) {
            log.error("Gagal menghubungi Brevo untuk {}: {}", tujuan, e.getMessage(), e);
            throw new EmailSendException("Terjadi kesalahan saat mengirim email", e);
        }
    }
}
