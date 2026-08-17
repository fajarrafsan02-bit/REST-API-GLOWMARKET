package com.projekfajar.akuntansi.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.akuntansi.dto.BukuBesarResponse;
import com.projekfajar.akuntansi.dto.JurnalResponse;
import com.projekfajar.akuntansi.dto.LabaRugiResponse;
import com.projekfajar.akuntansi.dto.NeracaResponse;
import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.service.LaporanAkuntansiService;
import com.projekfajar.akuntansi.service.LaporanExcelService;
import com.projekfajar.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/akuntansi/laporan")
@RequiredArgsConstructor
@Slf4j
public class LaporanAkuntansiController {

    private final LaporanAkuntansiService laporanService;
    private final LaporanExcelService excelService;

    /* ============================================================
       LABA RUGI
    ============================================================ */

    @GetMapping("/laba-rugi")
    public ResponseEntity<Map<String, Object>> labaRugi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.withDayOfMonth(1);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Laporan laba rugi berhasil diambil",
                "data", laporanService.labaRugi(awal, akhir)));
    }

    @GetMapping("/laba-rugi/excel")
    public ResponseEntity<byte[]> labaRugiExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai)
            throws IOException {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.withDayOfMonth(1);

        LabaRugiResponse laporan = laporanService.labaRugi(awal, akhir);

        return berkas(excelService.labaRugi(laporan),
                "Laba_Rugi_" + awal + "_" + akhir + ".xlsx");
    }

    /* ============================================================
       NERACA
    ============================================================ */

    @GetMapping("/neraca")
    public ResponseEntity<Map<String, Object>> neraca(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Neraca berhasil diambil",
                "data", laporanService.neraca(akhir)));
    }

    @GetMapping("/neraca/excel")
    public ResponseEntity<byte[]> neracaExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai)
            throws IOException {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        NeracaResponse laporan = laporanService.neraca(akhir);

        return berkas(excelService.neraca(laporan), "Neraca_" + akhir + ".xlsx");
    }

    /* ============================================================
       BUKU BESAR
    ============================================================ */

    @GetMapping("/buku-besar")
    public ResponseEntity<Map<String, Object>> bukuBesar(
            @RequestParam String kodeAkun,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai) {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.withDayOfMonth(1);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Buku besar berhasil diambil",
                "data", laporanService.bukuBesar(kodeAkun, awal, akhir)));
    }

    @GetMapping("/buku-besar/excel")
    public ResponseEntity<byte[]> bukuBesarExcel(
            @RequestParam String kodeAkun,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai)
            throws IOException {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.withDayOfMonth(1);

        BukuBesarResponse laporan = laporanService.bukuBesar(kodeAkun, awal, akhir);

        return berkas(excelService.bukuBesar(laporan),
                "Buku_Besar_" + kodeAkun + "_" + awal + "_" + akhir + ".xlsx");
    }

    /* ============================================================
       JURNAL UMUM
    ============================================================ */

    @GetMapping("/jurnal")
    public ResponseEntity<Map<String, Object>> jurnalUmum(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai,
            @RequestParam(required = false) String sumber) {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.withDayOfMonth(1);

        List<JurnalResponse> data = laporanService.jurnalUmum(awal, akhir, keSumber(sumber));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Jurnal umum berhasil diambil",
                "data", data,
                // Penjaga keseimbangan ikut dikirim supaya masalah pembukuan
                // terlihat di halaman laporan, bukan hanya di log server.
                "selisihDebitKredit", laporanService.selisihDebitKredit()));
    }

    @GetMapping("/jurnal/excel")
    public ResponseEntity<byte[]> jurnalUmumExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sampai,
            @RequestParam(required = false) String sumber) throws IOException {

        LocalDate akhir = sampai != null ? sampai : LocalDate.now();
        LocalDate awal = mulai != null ? mulai : akhir.withDayOfMonth(1);

        List<JurnalResponse> data = laporanService.jurnalUmum(awal, akhir, keSumber(sumber));

        return berkas(excelService.jurnalUmum(data, awal, akhir),
                "Jurnal_Umum_" + awal + "_" + akhir + ".xlsx");
    }

    private SumberJurnal keSumber(String sumber) {
        if (sumber == null || sumber.isBlank()) {
            return null;
        }

        try {
            return SumberJurnal.valueOf(sumber.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown journal source requested: {}", sumber);
            throw new BusinessException("Sumber jurnal tidak dikenal: " + sumber);
        }
    }

    private ResponseEntity<byte[]> berkas(byte[] isi, String namaBerkas) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", namaBerkas);
        headers.setContentLength(isi.length);

        return ResponseEntity.ok().headers(headers).body(isi);
    }
}
