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

import com.projekfajar.akuntansi.dto.BebanRequest;
import com.projekfajar.akuntansi.dto.BebanResponse;
import com.projekfajar.akuntansi.service.BebanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/akuntansi/beban")
@RequiredArgsConstructor
@Slf4j
public class BebanController {

    private final BebanService bebanService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> daftar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.minusMonths(12);

        log.info("GET /api/admin/akuntansi/beban - list expenses from {} to {}", awal, akhir);

        List<BebanResponse> data = bebanService.daftar(awal, akhir);

        log.info("Returned {} expense records", data.size());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daftar beban berhasil diambil",
                "data", data));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> buat(@Valid @RequestBody BebanRequest request) {
        log.info("POST /api/admin/akuntansi/beban - create expense akun={}, jumlah={}, tanggal={}",
                request.getKodeAkun(), request.getJumlah(), request.getTanggal());
        BebanResponse data = bebanService.buat(request);

        log.info("Expense created id={} akun={}", data.getId(), data.getKodeAkun());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Beban tercatat",
                "data", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> batalkan(
            @PathVariable Long id,
            @RequestParam(required = false) String alasan) {

        log.info("DELETE /api/admin/akuntansi/beban/{} - cancel expense, alasan={}", id, alasan);

        BebanResponse data = bebanService.batalkan(id, alasan);

        log.info("Expense id={} cancelled and reversed", id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Beban dibatalkan dan dijurnal balik",
                "data", data));
    }
}
