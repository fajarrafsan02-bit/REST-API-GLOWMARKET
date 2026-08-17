package com.projekfajar.akuntansi.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.akuntansi.dto.PembelianRequest;
import com.projekfajar.akuntansi.dto.PembelianResponse;
import com.projekfajar.akuntansi.service.PembelianService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/akuntansi/pembelian")
@RequiredArgsConstructor
@Slf4j
public class PembelianController {

    private final PembelianService pembelianService;

    /** Tanpa rentang tanggal, daftar dibatasi 12 bulan terakhir agar tidak menarik seluruh riwayat. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> daftar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.minusMonths(12);

        log.info("Listing purchases for period {} to {}", awal, akhir);

        List<PembelianResponse> data = pembelianService.daftar(awal, akhir);

        log.info("Purchase list returned {} rows for period {} to {}", data.size(), awal, akhir);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daftar pembelian berhasil diambil",
                "data", data));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> buat(@Valid @RequestBody PembelianRequest request) {
        log.info("Creating purchase: date={}, supplier={}, method={}, items={}",
                request.getTanggal(), request.getPemasok(), request.getMetode(),
                request.getItems() != null ? request.getItems().size() : 0);

        PembelianResponse data = pembelianService.buat(request);

        log.info("Purchase created: id={}, number={}, total={}",
                data.getId(), data.getNomor(), data.getTotal());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pembelian " + data.getNomor() + " tercatat, stok sudah ditambahkan",
                "data", data));
    }

    @PostMapping("/{id}/lunasi")
    public ResponseEntity<Map<String, Object>> lunasi(@PathVariable Long id) {
        log.info("Settling purchase debt: purchaseId={}", id);

        PembelianResponse data = pembelianService.lunasi(id);

        log.info("Purchase debt settled: number={}, total={}", data.getNomor(), data.getTotal());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Utang " + data.getNomor() + " dilunasi, kas sudah berkurang",
                "data", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> batalkan(
            @PathVariable Long id,
            @RequestParam(required = false) String alasan) {

        log.info("Cancelling purchase: purchaseId={}, reason={}", id, alasan);

        PembelianResponse data = pembelianService.batalkan(id, alasan);

        log.info("Purchase cancelled: number={}, total={}, stock restored",
                data.getNomor(), data.getTotal());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pembelian " + data.getNomor() + " dibatalkan, stok dikembalikan",
                "data", data));
    }
}
