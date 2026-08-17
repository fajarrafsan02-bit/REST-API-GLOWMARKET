package com.projekfajar.produk.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.service.PenyesuaianStokService;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.produk.dto.ProdukVarianRequest;
import com.projekfajar.produk.dto.ProdukVarianResponse;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.ProdukVarian;
import com.projekfajar.produk.repository.ProdukVarianRepository;
import com.projekfajar.produk.repository.ProdukRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CRUD varian produk. Perubahan stok varian dicatat sebagai penyesuaian
 * persediaan seperti halnya produk induk, agar pembukuan tetap sinkron.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProdukVarianService {

    private final ProdukVarianRepository varianRepository;
    private final ProdukRepository produkRepository;
    private final PenyesuaianStokService penyesuaianStokService;
    private final com.projekfajar.restock.service.RestockNotifikasiService restockNotifikasiService;

    @Transactional(readOnly = true)
    public List<ProdukVarianResponse> getByProduk(Long produkId, boolean hanyaAktif) {
        log.debug("Fetching variants for productId={} activeOnly={}", produkId, hanyaAktif);
        List<ProdukVarian> list = hanyaAktif
                ? varianRepository.findByProdukIdAndAktifTrueOrderByIdAsc(produkId)
                : varianRepository.findByProdukIdOrderByIdAsc(produkId);
        log.debug("Found {} variants for productId={}", list.size(), produkId);
        return list.stream().map(ProdukVarianResponse::from).toList();
    }

    @Transactional
    public ProdukVarianResponse create(Long produkId, ProdukVarianRequest request) {
        log.info("Creating variant for productId={} nama={} harga={} stock={}",
                produkId, request.getNama(), request.getHarga(), request.getStock());
        Produk produk = produkRepository.findByIdAndDeletedFalse(produkId)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan"));

        ProdukVarian varian = ProdukVarian.builder()
                .produk(produk)
                .nama(request.getNama().trim())
                .harga(request.getHarga())
                .hargaModal(request.getHargaModal() != null ? request.getHargaModal() : BigDecimal.ZERO)
                .stock(request.getStock())
                .aktif(request.getAktif() != null ? request.getAktif() : true)
                .createdAt(LocalDateTime.now())
                .build();

        ProdukVarian saved = varianRepository.save(varian);
        log.info("Variant created: variantId={} nama={} productId={} stock={}",
                saved.getId(), saved.getNama(), produkId, saved.getStock());

        penyesuaianStokService.catat(produk, 0, saved.getStock(),
                "stok awal varian " + saved.getNama());

        return ProdukVarianResponse.from(saved);
    }

    @Transactional
    public ProdukVarianResponse update(Long varianId, ProdukVarianRequest request) {
        log.info("Updating variantId={} nama={} harga={} stock={}",
                varianId, request.getNama(), request.getHarga(), request.getStock());
        ProdukVarian varian = varianRepository.findById(varianId)
                .orElseThrow(() -> new ResourceNotFoundException("Varian tidak ditemukan"));

        Integer stokLama = varian.getStock();
        BigDecimal hargaModalLama = varian.getHargaModal();

        varian.setNama(request.getNama().trim());
        varian.setHarga(request.getHarga());
        if (request.getHargaModal() != null) {
            varian.setHargaModal(request.getHargaModal());
        }
        varian.setStock(request.getStock());
        varian.setAktif(request.getAktif() != null ? request.getAktif() : varian.getAktif());
        varian.setUpdatedAt(LocalDateTime.now());

        ProdukVarian saved = varianRepository.save(varian);
        log.info("Variant updated: variantId={} nama={} stock={} active={}",
                saved.getId(), saved.getNama(), saved.getStock(), saved.getAktif());

        if (!stokLama.equals(request.getStock())) {
            log.info("Variant stock changed: variantId={} from={} to={}",
                    saved.getId(), stokLama, request.getStock());
            penyesuaianStokService.catat(saved.getProduk(), stokLama, request.getStock(),
                    "stok varian " + saved.getNama());
            // Varian kembali tersedia → beri tahu pendaftar "beri tahu saya".
            restockNotifikasiService.cekRestock(saved.getProduk());
        }

        return ProdukVarianResponse.from(saved);
    }

    @Transactional
    public void delete(Long varianId) {
        log.info("Deactivating variantId={}", varianId);
        ProdukVarian varian = varianRepository.findById(varianId)
                .orElseThrow(() -> new ResourceNotFoundException("Varian tidak ditemukan"));

        // Keranjang yang masih menyimpan varian ini akan mengarah ke baris
        // yang hilang — cegah dengan menonaktifkan saja kalau masih terpakai.
        varian.setAktif(false);
        varian.setUpdatedAt(LocalDateTime.now());
        varianRepository.save(varian);

        log.info("Variant deactivated: variantId={} nama={}", varianId, varian.getNama());
    }

    /** Dipakai saat produk dihapus: nonaktifkan seluruh variannya. */
    @Transactional
    public void nonaktifkanSemua(Long produkId) {
        log.info("Deactivating all variants of productId={}", produkId);
        List<ProdukVarian> varianList = varianRepository.findByProdukIdOrderByIdAsc(produkId);
        varianList.forEach(v -> v.setAktif(false));
        if (!varianList.isEmpty()) {
            varianRepository.saveAll(varianList);
            log.info("Deactivated {} variants of productId={}", varianList.size(), produkId);
        }
    }
}
