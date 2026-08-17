package com.projekfajar.ongkir.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.projekfajar.ongkir.dto.RajaOngkirCostResponse;

/**
 * Satu-satunya kelas yang bicara langsung ke API RajaOngkir. Tidak pernah
 * melempar exception ke pemanggil — kegagalan apa pun (kunci kosong, jaringan,
 * kuota harian habis, respons kosong) selalu berakhir sebagai Optional.empty()
 * atau list kosong, supaya OngkirCalculationService bisa jatuh ke tarif tetap
 * tanpa pengecualian khusus di sisi pemanggil.
 */
@Service
public class RajaOngkirClient {
    private static final Logger logger = LoggerFactory.getLogger(RajaOngkirClient.class);

    private final WebClient rajaOngkirWebClient;

    @Value("${rajaongkir.api-key:}")
    private String apiKey;

    public RajaOngkirClient(@Qualifier("rajaOngkirWebClient") WebClient rajaOngkirWebClient) {
        this.rajaOngkirWebClient = rajaOngkirWebClient;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Cari lokasi (sampai level kecamatan) dari teks bebas — dipakai baik
     * untuk mencocokkan alamat pembeli maupun mencari lokasi asal toko di
     * pengaturan admin. Bentuk item hasil pencarian belum terverifikasi penuh
     * dari dokumentasi publik, jadi sengaja dikembalikan mentah sebagai
     * Map alih-alih di-bind ke DTO kaku yang bisa salah tebak nama field-nya.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> cariLokasi(String kataKunci) {
        if (!isConfigured() || kataKunci == null || kataKunci.isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> body = rajaOngkirWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/destination/domestic-destination")
                            .queryParam("search", kataKunci)
                            .queryParam("limit", 10)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Object data = body != null ? body.get("data") : null;
            if (data instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            logger.warn("Pencarian lokasi RajaOngkir gagal untuk kata kunci '{}': {}", kataKunci, e.getMessage());
            return List.of();
        }
    }

    /** Hitung ongkir domestik. Kosong kalau API tidak dikonfigurasi, request gagal, kuota habis, atau tidak ada kurir yang cocok. */
    public Optional<List<RajaOngkirCostResponse.Tarif>> hitungOngkir(
            String originId, String destinationId, int beratGram, List<String> kurir) {

        if (!isConfigured() || originId == null || destinationId == null || kurir == null || kurir.isEmpty()) {
            return Optional.empty();
        }

        try {
            // Dokumentasi Komerce: application/x-www-form-urlencoded, bukan JSON.
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("origin", originId);
            form.add("destination", destinationId);
            form.add("weight", String.valueOf(Math.max(beratGram, 1)));
            form.add("courier", String.join(":", kurir));

            RajaOngkirCostResponse response = rajaOngkirWebClient.post()
                    .uri("/calculate/domestic-cost")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(RajaOngkirCostResponse.class)
                    .block();

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                logger.warn("Hitung ongkir RajaOngkir kosong (origin={}, destination={}, kurir={})",
                        originId, destinationId, kurir);
                return Optional.empty();
            }
            return Optional.of(response.getData());
        } catch (Exception e) {
            logger.warn("Hitung ongkir RajaOngkir gagal (origin={}, destination={}): {}",
                    originId, destinationId, e.getMessage());
            return Optional.empty();
        }
    }
}
