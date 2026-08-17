package com.projekfajar.ongkir.service;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.ongkir.model.Ongkir;
import com.projekfajar.ongkir.repository.OngkirRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OngkirService {

    public static final List<String> PROVINSI_SEED = List.of(
            "Aceh", "Sumatera Utara", "Sumatera Barat", "Riau", "Jambi",
            "Sumatera Selatan", "Bengkulu", "Lampung", "Kepulauan Bangka Belitung",
            "Kepulauan Riau", "DKI Jakarta", "Jawa Barat", "Jawa Tengah",
            "DI Yogyakarta", "Jawa Timur", "Banten", "Bali", "Nusa Tenggara Barat",
            "Nusa Tenggara Timur", "Kalimantan Barat", "Kalimantan Tengah",
            "Kalimantan Selatan", "Kalimantan Timur", "Kalimantan Utara",
            "Sulawesi Utara", "Sulawesi Tengah", "Sulawesi Selatan",
            "Sulawesi Tenggara", "Gorontalo", "Sulawesi Barat", "Maluku",
            "Maluku Utara", "Papua Barat", "Papua");

    private final OngkirRepository ongkirRepository;

    @Transactional(readOnly = true)
    public List<Ongkir> getAll() {
        return ongkirRepository.findAllByOrderByProvinsiAsc();
    }

    @Transactional(readOnly = true)
    public Ongkir getById(Long id) {
        return ongkirRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif ongkir tidak ditemukan"));
    }

    @Transactional(readOnly = true)
    public Ongkir getByProvinsi(String provinsi) {
        return ongkirRepository.findByProvinsi(provinsi)
                .orElse(null);
    }

    @Transactional
    public Ongkir create(Map<String, Object> request) {
        String provinsi = String.valueOf(request.get("provinsi")).trim();
        if (provinsi.isEmpty()) {
            throw new BusinessException("Provinsi harus diisi");
        }

        if (ongkirRepository.findByProvinsi(provinsi).isPresent()) {
            throw new BusinessException("Tarif ongkir untuk provinsi tersebut sudah ada");
        }

        BigDecimal tarif = parseTarif(request.get("tarif"));
        Integer estimasiHari = parseEstimasi(request.get("estimasiHari"));

        Ongkir ongkir = Ongkir.builder()
                .provinsi(provinsi)
                .tarif(tarif)
                .estimasiHari(estimasiHari)
                .build();

        Ongkir saved = ongkirRepository.save(ongkir);
        log.info("Ongkir created for province: {}", provinsi);
        return saved;
    }

    @Transactional
    public Ongkir update(Long id, Map<String, Object> request) {
        Ongkir ongkir = ongkirRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif ongkir tidak ditemukan"));

        if (request.containsKey("provinsi")) {
            String provinsi = String.valueOf(request.get("provinsi")).trim();
            if (provinsi.isEmpty()) {
                throw new BusinessException("Provinsi tidak boleh kosong");
            }
            ongkirRepository.findByProvinsi(provinsi)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BusinessException("Tarif ongkir untuk provinsi tersebut sudah ada");
                    });
            ongkir.setProvinsi(provinsi);
        }

        if (request.containsKey("tarif")) {
            ongkir.setTarif(parseTarif(request.get("tarif")));
        }

        if (request.containsKey("estimasiHari")) {
            ongkir.setEstimasiHari(parseEstimasi(request.get("estimasiHari")));
        }

        Ongkir saved = ongkirRepository.save(ongkir);
        log.info("Ongkir updated for province: {}", saved.getProvinsi());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Ongkir ongkir = ongkirRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarif ongkir tidak ditemukan"));
        ongkirRepository.delete(ongkir);
        log.info("Ongkir deleted for province: {}", ongkir.getProvinsi());
    }

    @Transactional
    public void seedDefaults() {
        if (ongkirRepository.count() > 0) {
            return;
        }

        for (String provinsi : PROVINSI_SEED) {
            ongkirRepository.save(Ongkir.builder()
                    .provinsi(provinsi)
                    .tarif(BigDecimal.ZERO)
                    .estimasiHari(3)
                    .build());
        }
        log.info("Seeded {} ongkir defaults", PROVINSI_SEED.size());
    }

    private BigDecimal parseTarif(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new BusinessException("Tarif ongkir harus diisi");
        }
        try {
            BigDecimal tarif = new BigDecimal(String.valueOf(value).trim());
            if (tarif.signum() < 0) {
                throw new BusinessException("Tarif ongkir tidak boleh negatif");
            }
            return tarif.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            log.warn("Invalid shipping rate value '{}': not a number", value);
            throw new BusinessException("Tarif ongkir harus berupa angka");
        }
    }

    private Integer parseEstimasi(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            Integer estimasi = Integer.parseInt(String.valueOf(value).trim());
            if (estimasi < 0) {
                throw new BusinessException("Estimasi hari tidak boleh negatif");
            }
            return estimasi;
        } catch (NumberFormatException e) {
            log.warn("Invalid estimated days value '{}': not a number", value);
            throw new BusinessException("Estimasi hari harus berupa angka");
        }
    }
}