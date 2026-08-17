package com.projekfajar.terjual.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.exception.ProdukNotFoundException;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.terjual.dto.TerjualProdukResponse;
import com.projekfajar.terjual.repository.ProdukTerjualRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jumlah produk terjual dihitung dari catatan transaksi (produk_terjual).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TerjualProdukService {

    private final ProdukTerjualRepository produkTerjualRepository;
    private final ProdukRepository produkRepository;

    @Transactional(readOnly = true)
    public List<TerjualProdukResponse> getAll() {
        log.info("Calculating total sold products summary from transaction records");
        Map<Long, Integer> terjualPerProduk = new HashMap<>();

        for (Object[] row : produkTerjualRepository.sumQtyGroupByProduk()) {
            Long produkId = ((Number) row[0]).longValue();
            Integer jumlah = ((Number) row[1]).intValue();
            terjualPerProduk.put(produkId, jumlah);
        }

        return produkRepository.findByDeletedFalse().stream()
                .map(produk -> toResponse(produk, terjualPerProduk.getOrDefault(produk.getId(), 0)))
                .sorted(Comparator.comparing(TerjualProdukResponse::getTerjual).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public TerjualProdukResponse getByProdukId(Long produkId) {
        log.info("Calculating total sold count for product ID: {}", produkId);
        Produk produk = produkRepository.findById(produkId)
                .orElseThrow(() -> {
                    log.warn("Fetch sold count failed: Product ID {} not found", produkId);
                    return new ProdukNotFoundException("Data tidak ditemukan untuk produk ID: " + produkId);
                });

        Integer terjual = produkTerjualRepository.getTotalTerjualByProduk(produkId);

        return toResponse(produk, terjual != null ? terjual : 0);
    }

    private TerjualProdukResponse toResponse(Produk produk, Integer terjual) {
        return TerjualProdukResponse.builder()
                .id(produk.getId())
                .produkId(produk.getId())
                .namaProduk(produk.getNama())
                .gambar(produk.getGambar())
                .harga(produk.getHarga())
                .terjual(terjual)
                .karatEmas(produk.getKaratEmas())
                .build();
    }
}
