package com.projekfajar.wilayah.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

/**
 * Proxy ke API wilayah Indonesia (emsifa, data provinsi/kota/kecamatan/
 * kelurahan) dan pencarian kode pos (kodepos.vercel.app) — dipanggil dari
 * backend, bukan langsung dari browser pembeli. Alasannya bukan soal kunci
 * API (keduanya gratis tanpa kunci), tapi supaya form alamat tidak lagi
 * bergantung pada DNS/jaringan milik pengguna ke domain pihak ketiga; kalau
 * gagal, selalu list kosong — form alamat tetap bisa dipakai secara manual.
 */
@Service
@RequiredArgsConstructor
public class WilayahClient {
    private static final Logger logger = LoggerFactory.getLogger(WilayahClient.class);

    private static final String WILAYAH_BASE = "https://www.emsifa.com/api-wilayah-indonesia/api";

    private final WebClient wilayahWebClient;

    public List<Map<String, Object>> provinces() {
        return ambilDaftar(WILAYAH_BASE + "/provinces.json");
    }

    public List<Map<String, Object>> regencies(String provinceId) {
        return ambilDaftar(WILAYAH_BASE + "/regencies/" + provinceId + ".json");
    }

    public List<Map<String, Object>> districts(String regencyId) {
        return ambilDaftar(WILAYAH_BASE + "/districts/" + regencyId + ".json");
    }

    public List<Map<String, Object>> villages(String districtId) {
        return ambilDaftar(WILAYAH_BASE + "/villages/" + districtId + ".json");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> cariKodePos(String query) {
        try {
            Map<String, Object> body = wilayahWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("kodepos.vercel.app").path("/search")
                            .queryParam("q", query)
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
            logger.warn("Pencarian kode pos gagal untuk '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> ambilDaftar(String url) {
        try {
            List<?> body = wilayahWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (body == null) {
                return List.of();
            }

            return body.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        } catch (Exception e) {
            logger.warn("Gagal memuat data wilayah dari {}: {}", url, e.getMessage());
            return List.of();
        }
    }
}
