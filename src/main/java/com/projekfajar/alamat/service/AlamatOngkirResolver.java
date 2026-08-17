package com.projekfajar.alamat.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.alamat.repository.AlamatRepository;
import com.projekfajar.ongkir.service.RajaOngkirClient;

import lombok.RequiredArgsConstructor;

/**
 * Mencocokkan alamat pembeli (teks bebas: kelurahan/kecamatan/kota/provinsi)
 * ke ID kecamatan milik RajaOngkir, lalu menyimpan hasilnya balik ke baris
 * Alamat supaya pemakaian berikutnya tidak perlu mencari ulang — satu kali
 * pencarian per alamat seumur hidupnya, bukan per transaksi, karena
 * pencarian ini yang paling memakan kuota harian API.
 */
@Service
@RequiredArgsConstructor
public class AlamatOngkirResolver {
    private static final Logger logger = LoggerFactory.getLogger(AlamatOngkirResolver.class);

    private final RajaOngkirClient rajaOngkirClient;
    private final AlamatRepository alamatRepository;

    @Transactional
    public Optional<String> resolveDestinationId(Alamat alamat) {
        if (alamat == null) {
            return Optional.empty();
        }

        String cached = alamat.getRajaongkirDestinationId();
        if (cached != null && !cached.isBlank()) {
            return Optional.of(cached);
        }

        String kataKunci = String.join(" ",
                nonBlank(alamat.getKelurahan()),
                nonBlank(alamat.getKecamatan()),
                nonBlank(alamat.getKota())).trim();

        if (kataKunci.isBlank()) {
            return Optional.empty();
        }

        List<Map<String, Object>> hasil = rajaOngkirClient.cariLokasi(kataKunci);
        Optional<String> id = pilihTerbaik(hasil, alamat.getProvinsi());

        if (id.isPresent()) {
            alamat.setRajaongkirDestinationId(id.get());
            alamatRepository.save(alamat);
            logger.info("Alamat {} dicocokkan ke lokasi RajaOngkir {}", alamat.getId(), id.get());
        } else {
            logger.warn("Alamat {} tidak bisa dicocokkan ke lokasi RajaOngkir (kata kunci: '{}')",
                    alamat.getId(), kataKunci);
        }

        return id;
    }

    /**
     * Bentuk field hasil pencarian belum terverifikasi penuh dari dokumentasi
     * publik, jadi dicari secara defensif: ambil hasil yang salah satu
     * nilainya menyebut nama provinsi alamat (kalau provinsinya diisi), atau
     * hasil pertama apa adanya kalau tidak ada yang cocok lebih ketat.
     */
    private Optional<String> pilihTerbaik(List<Map<String, Object>> hasil, String provinsi) {
        if (hasil.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> dipilih = hasil.get(0);
        if (provinsi != null && !provinsi.isBlank()) {
            String provinsiDicari = provinsi.trim().toLowerCase();
            dipilih = hasil.stream()
                    .filter(item -> item.values().stream()
                            .filter(String.class::isInstance)
                            .map(v -> ((String) v).toLowerCase())
                            .anyMatch(v -> v.equals(provinsiDicari) || v.contains(provinsiDicari)))
                    .findFirst()
                    .orElse(dipilih);
        }

        return ekstrakId(dipilih);
    }

    private Optional<String> ekstrakId(Map<String, Object> item) {
        Object id = item.get("id");
        if (id != null) {
            return Optional.of(String.valueOf(id));
        }
        return item.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("id") && e.getValue() != null)
                .map(e -> String.valueOf(e.getValue()))
                .findFirst();
    }

    private String nonBlank(String value) {
        return value != null ? value : "";
    }
}
