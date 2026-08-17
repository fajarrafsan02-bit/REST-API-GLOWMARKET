package com.projekfajar.akuntansi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.akuntansi.dto.SaldoAwalRequest;
import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.repository.AkunRepository;
import com.projekfajar.akuntansi.service.SaldoAwalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Semua jalur di bawah /api/admin sudah dibatasi hasRole("ADMIN") oleh SecurityConfig,
 * jadi controller ini tidak mengulang pemeriksaan peran.
 */
@RestController
@RequestMapping("/api/admin/akuntansi")
@RequiredArgsConstructor
@Slf4j
public class AkuntansiController {

    private final AkunRepository akunRepository;
    private final SaldoAwalService saldoAwalService;

    @GetMapping("/akun")
    public ResponseEntity<Map<String, Object>> daftarAkun() {
        log.info("GET /api/admin/akuntansi/akun - list active accounts");
        List<Akun> akun = akunRepository.findByAktifTrueOrderByKodeAsc();
        log.info("Returned {} active accounts", akun.size());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daftar akun berhasil diambil",
                "data", akun));
    }

    @GetMapping("/saldo-awal")
    public ResponseEntity<Map<String, Object>> infoSaldoAwal() {
        log.info("GET /api/admin/akuntansi/saldo-awal - fetch opening balance info");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Info saldo awal berhasil diambil",
                "data", saldoAwalService.info()));
    }

    @PostMapping("/saldo-awal")
    public ResponseEntity<Map<String, Object>> catatSaldoAwal(@RequestBody SaldoAwalRequest request) {
        log.info("POST /api/admin/akuntansi/saldo-awal - record opening balance, tanggal={}, kas={}",
                request.getTanggal(), request.getKas());
        Jurnal jurnal = saldoAwalService.catat(request);
        log.info("Opening balance recorded as jurnal {} total={}", jurnal.getNomor(), jurnal.totalDebit());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Saldo awal tercatat pada jurnal " + jurnal.getNomor(),
                "data", Map.of(
                        "nomor", jurnal.getNomor(),
                        "tanggal", jurnal.getTanggal(),
                        "total", jurnal.totalDebit())));
    }
}
