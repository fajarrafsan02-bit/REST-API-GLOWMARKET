package com.projekfajar.akuntansi.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.projekfajar.akuntansi.dto.BukuBesarResponse;
import com.projekfajar.akuntansi.dto.JurnalResponse;
import com.projekfajar.akuntansi.dto.LabaRugiResponse;
import com.projekfajar.akuntansi.dto.LabaRugiResponse.BarisAkun;
import com.projekfajar.akuntansi.dto.NeracaResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Export laporan ke Excel, mengikuti gaya berkas yang sudah dipakai laporan
 * penjualan supaya berkas akuntansi tidak terasa asing di samping yang lama.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LaporanExcelService {

    private static final DateTimeFormatter TANGGAL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] labaRugi(LabaRugiResponse laporan) throws IOException {
        log.info("Exporting profit and loss report to Excel for period {} to {}",
                laporan.getMulai(), laporan.getSampai());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Laba Rugi");
            Gaya gaya = new Gaya(workbook);

            int baris = judul(sheet, gaya, "LAPORAN LABA RUGI",
                    "Periode " + laporan.getMulai().format(TANGGAL)
                            + " s/d " + laporan.getSampai().format(TANGGAL),
                    2);

            baris = kepalaTabel(sheet, gaya, baris, "Keterangan", "Jumlah (Rp)");

            baris = golongan(sheet, gaya, baris, "PENDAPATAN",
                    laporan.getPendapatan(), "Total Pendapatan", laporan.getTotalPendapatan());

            baris = golongan(sheet, gaya, baris, "HARGA POKOK PENJUALAN",
                    laporan.getHpp(), "Total HPP", laporan.getTotalHpp());

            baris = barisTotal(sheet, gaya, baris, "LABA KOTOR", laporan.getLabaKotor());
            baris++;

            baris = golongan(sheet, gaya, baris, "BEBAN OPERASIONAL",
                    laporan.getBeban(), "Total Beban", laporan.getTotalBeban());

            baris = barisTotal(sheet, gaya, baris, "LABA BERSIH", laporan.getLabaBersih());

            if (laporan.getPenjualanTanpaHpp() > 0) {
                baris++;
                XSSFCell catatan = sheet.createRow(baris).createCell(0);
                catatan.setCellValue("Catatan: " + laporan.getPenjualanTanpaHpp()
                        + " penjualan belum punya HPP karena harga modal produknya belum diisi. "
                        + "Laba di atas lebih besar dari yang sebenarnya.");
                catatan.setCellStyle(gaya.catatan);
            }

            sheet.setColumnWidth(0, 12000);
            sheet.setColumnWidth(1, 6000);

            byte[] berkas = keBytes(workbook);

            log.info("Profit and loss Excel generated for {} to {}: {} bytes",
                    laporan.getMulai(), laporan.getSampai(), berkas.length);

            return berkas;
        }
    }

    public byte[] neraca(NeracaResponse laporan) throws IOException {
        log.info("Exporting balance sheet to Excel as of {}", laporan.getSampai());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Neraca");
            Gaya gaya = new Gaya(workbook);

            int baris = judul(sheet, gaya, "NERACA",
                    "Per " + laporan.getSampai().format(TANGGAL), 2);

            baris = kepalaTabel(sheet, gaya, baris, "Keterangan", "Jumlah (Rp)");

            baris = golongan(sheet, gaya, baris, "ASET",
                    laporan.getAset(), "Total Aset", laporan.getTotalAset());

            baris = golongan(sheet, gaya, baris, "LIABILITAS",
                    laporan.getLiabilitas(), "Total Liabilitas", laporan.getTotalLiabilitas());

            baris = seksi(sheet, gaya, baris, "EKUITAS");
            for (BarisAkun akun : laporan.getEkuitas()) {
                baris = barisAkun(sheet, gaya, baris, akun);
            }
            baris = barisBiasa(sheet, gaya, baris, "Laba Tahun Berjalan",
                    laporan.getLabaTahunBerjalan());
            baris = barisSubtotal(sheet, gaya, baris, "Total Ekuitas", laporan.getTotalEkuitas());
            baris++;

            baris = barisTotal(sheet, gaya, baris, "TOTAL LIABILITAS + EKUITAS",
                    laporan.getTotalLiabilitasDanEkuitas());

            if (!laporan.isSeimbang()) {
                log.warn("Balance sheet export as of {} is not balanced, difference={}",
                        laporan.getSampai(), laporan.getSelisih());
                baris++;
                XSSFCell catatan = sheet.createRow(baris).createCell(0);
                catatan.setCellValue("PERINGATAN: neraca tidak seimbang, selisih "
                        + laporan.getSelisih() + ". Periksa jurnal.");
                catatan.setCellStyle(gaya.catatan);
            }

            sheet.setColumnWidth(0, 12000);
            sheet.setColumnWidth(1, 6000);

            byte[] berkas = keBytes(workbook);

            log.info("Balance sheet Excel generated as of {}: {} bytes",
                    laporan.getSampai(), berkas.length);

            return berkas;
        }
    }

    public byte[] bukuBesar(BukuBesarResponse laporan) throws IOException {
        log.info("Exporting general ledger to Excel: account={}, period {} to {}, {} mutations",
                laporan.getKodeAkun(), laporan.getMulai(), laporan.getSampai(),
                laporan.getMutasi().size());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Buku Besar");
            Gaya gaya = new Gaya(workbook);

            int baris = judul(sheet, gaya,
                    "BUKU BESAR — " + laporan.getKodeAkun() + " " + laporan.getNamaAkun(),
                    "Periode " + laporan.getMulai().format(TANGGAL)
                            + " s/d " + laporan.getSampai().format(TANGGAL),
                    5);

            baris = kepalaTabel(sheet, gaya, baris,
                    "Tanggal", "No. Jurnal", "Keterangan", "Debit", "Kredit", "Saldo");

            XSSFRow awal = sheet.createRow(baris++);
            teks(awal, 2, "Saldo awal", gaya.selSubtotal);
            uang(awal, 5, laporan.getSaldoAwal(), gaya.uangSubtotal);

            for (BukuBesarResponse.Mutasi mutasi : laporan.getMutasi()) {
                XSSFRow row = sheet.createRow(baris++);
                teks(row, 0, mutasi.getTanggal().format(TANGGAL), gaya.sel);
                teks(row, 1, mutasi.getNomorJurnal(), gaya.sel);
                teks(row, 2, mutasi.getKeterangan(), gaya.sel);
                uang(row, 3, mutasi.getDebit(), gaya.uang);
                uang(row, 4, mutasi.getKredit(), gaya.uang);
                uang(row, 5, mutasi.getSaldo(), gaya.uang);
            }

            XSSFRow akhir = sheet.createRow(baris);
            teks(akhir, 2, "Total", gaya.selTotal);
            uang(akhir, 3, laporan.getTotalDebit(), gaya.uangTotal);
            uang(akhir, 4, laporan.getTotalKredit(), gaya.uangTotal);
            uang(akhir, 5, laporan.getSaldoAkhir(), gaya.uangTotal);

            lebarKolom(sheet, 3000, 5000, 12000, 5000, 5000, 5000);

            byte[] berkas = keBytes(workbook);

            log.info("General ledger Excel generated for account {}: {} bytes",
                    laporan.getKodeAkun(), berkas.length);

            return berkas;
        }
    }

    public byte[] jurnalUmum(List<JurnalResponse> jurnalList, LocalDate mulai, LocalDate sampai)
            throws IOException {

        log.info("Exporting general journal to Excel: period {} to {}, {} entries",
                mulai, sampai, jurnalList.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Jurnal Umum");
            Gaya gaya = new Gaya(workbook);

            int baris = judul(sheet, gaya, "JURNAL UMUM",
                    "Periode " + mulai.format(TANGGAL) + " s/d " + sampai.format(TANGGAL), 6);

            baris = kepalaTabel(sheet, gaya, baris,
                    "Tanggal", "No. Jurnal", "Sumber", "Akun", "Debit", "Kredit", "Keterangan");

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalKredit = BigDecimal.ZERO;

            for (JurnalResponse jurnal : jurnalList) {
                for (JurnalResponse.Baris b : jurnal.getBaris()) {
                    XSSFRow row = sheet.createRow(baris++);
                    teks(row, 0, jurnal.getTanggal().format(TANGGAL), gaya.sel);
                    teks(row, 1, jurnal.getNomor(), gaya.sel);
                    teks(row, 2, jurnal.getSumber(), gaya.sel);
                    teks(row, 3, b.getKodeAkun() + " " + b.getNamaAkun(), gaya.sel);
                    uang(row, 4, b.getDebit(), gaya.uang);
                    uang(row, 5, b.getKredit(), gaya.uang);
                    teks(row, 6, b.getKeterangan() != null ? b.getKeterangan()
                            : jurnal.getKeterangan(), gaya.sel);

                    totalDebit = totalDebit.add(b.getDebit());
                    totalKredit = totalKredit.add(b.getKredit());
                }
            }

            XSSFRow total = sheet.createRow(baris);
            teks(total, 3, "Total", gaya.selTotal);
            uang(total, 4, totalDebit, gaya.uangTotal);
            uang(total, 5, totalKredit, gaya.uangTotal);

            lebarKolom(sheet, 3000, 5000, 3500, 8000, 5000, 5000, 10000);

            byte[] berkas = keBytes(workbook);

            log.info("General journal Excel generated for {} to {}: totalDebit={}, totalCredit={}, {} bytes",
                    mulai, sampai, totalDebit, totalKredit, berkas.length);

            return berkas;
        }
    }

    /* ============================================================
       PEMBANTU TATA LETAK
    ============================================================ */

    private int judul(XSSFSheet sheet, Gaya gaya, String judul, String subjudul, int kolomAkhir) {
        XSSFRow barisJudul = sheet.createRow(0);
        barisJudul.setHeightInPoints(30);
        XSSFCell sel = barisJudul.createCell(0);
        sel.setCellValue(judul);
        sel.setCellStyle(gaya.judul);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, kolomAkhir));

        XSSFRow barisSub = sheet.createRow(1);
        XSSFCell selSub = barisSub.createCell(0);
        selSub.setCellValue(subjudul);
        selSub.setCellStyle(gaya.subjudul);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, kolomAkhir));

        return 3;
    }

    private int kepalaTabel(XSSFSheet sheet, Gaya gaya, int baris, String... kepala) {
        XSSFRow row = sheet.createRow(baris);
        for (int i = 0; i < kepala.length; i++) {
            teks(row, i, kepala[i], gaya.kepala);
        }
        return baris + 1;
    }

    private int golongan(XSSFSheet sheet, Gaya gaya, int baris, String nama,
            List<BarisAkun> akunList, String labelTotal, BigDecimal total) {

        baris = seksi(sheet, gaya, baris, nama);

        for (BarisAkun akun : akunList) {
            baris = barisAkun(sheet, gaya, baris, akun);
        }

        if (akunList.isEmpty()) {
            XSSFRow kosong = sheet.createRow(baris++);
            teks(kosong, 0, "  (belum ada catatan)", gaya.sel);
            uang(kosong, 1, BigDecimal.ZERO, gaya.uang);
        }

        baris = barisSubtotal(sheet, gaya, baris, labelTotal, total);
        return baris + 1;
    }

    private int seksi(XSSFSheet sheet, Gaya gaya, int baris, String nama) {
        XSSFRow row = sheet.createRow(baris);
        teks(row, 0, nama, gaya.seksi);
        teks(row, 1, "", gaya.seksi);
        return baris + 1;
    }

    private int barisAkun(XSSFSheet sheet, Gaya gaya, int baris, BarisAkun akun) {
        return barisBiasa(sheet, gaya, baris,
                "  " + akun.getKode() + " " + akun.getNama(), akun.getJumlah());
    }

    private int barisBiasa(XSSFSheet sheet, Gaya gaya, int baris, String label, BigDecimal nilai) {
        XSSFRow row = sheet.createRow(baris);
        teks(row, 0, label, gaya.sel);
        uang(row, 1, nilai, gaya.uang);
        return baris + 1;
    }

    private int barisSubtotal(XSSFSheet sheet, Gaya gaya, int baris, String label,
            BigDecimal nilai) {
        XSSFRow row = sheet.createRow(baris);
        teks(row, 0, label, gaya.selSubtotal);
        uang(row, 1, nilai, gaya.uangSubtotal);
        return baris + 1;
    }

    private int barisTotal(XSSFSheet sheet, Gaya gaya, int baris, String label, BigDecimal nilai) {
        XSSFRow row = sheet.createRow(baris);
        teks(row, 0, label, gaya.selTotal);
        uang(row, 1, nilai, gaya.uangTotal);
        return baris + 1;
    }

    private void teks(XSSFRow row, int kolom, String nilai, XSSFCellStyle gaya) {
        XSSFCell sel = row.createCell(kolom);
        sel.setCellValue(nilai != null ? nilai : "");
        sel.setCellStyle(gaya);
    }

    private void uang(XSSFRow row, int kolom, BigDecimal nilai, XSSFCellStyle gaya) {
        XSSFCell sel = row.createCell(kolom);
        sel.setCellValue(nilai != null ? nilai.doubleValue() : 0d);
        sel.setCellStyle(gaya);
    }

    private void lebarKolom(XSSFSheet sheet, int... lebar) {
        for (int i = 0; i < lebar.length; i++) {
            sheet.setColumnWidth(i, lebar[i]);
        }
    }

    private byte[] keBytes(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    /** Kumpulan gaya sel, dibuat sekali per workbook karena jumlah style Excel terbatas. */
    private static class Gaya {

        private final XSSFCellStyle judul;
        private final XSSFCellStyle subjudul;
        private final XSSFCellStyle kepala;
        private final XSSFCellStyle seksi;
        private final XSSFCellStyle sel;
        private final XSSFCellStyle uang;
        private final XSSFCellStyle selSubtotal;
        private final XSSFCellStyle uangSubtotal;
        private final XSSFCellStyle selTotal;
        private final XSSFCellStyle uangTotal;
        private final XSSFCellStyle catatan;

        Gaya(XSSFWorkbook workbook) {
            XSSFColor emas = new XSSFColor(new java.awt.Color(184, 134, 11), null);
            XSSFColor abu = new XSSFColor(new java.awt.Color(240, 240, 240), null);
            short format = workbook.createDataFormat().getFormat("#,##0.00");

            XSSFFont fontJudul = workbook.createFont();
            fontJudul.setFontName("Calibri");
            fontJudul.setFontHeightInPoints((short) 16);
            fontJudul.setBold(true);
            fontJudul.setColor(IndexedColors.WHITE.getIndex());

            XSSFFont fontTebal = workbook.createFont();
            fontTebal.setFontName("Calibri");
            fontTebal.setFontHeightInPoints((short) 11);
            fontTebal.setBold(true);

            XSSFFont fontKepala = workbook.createFont();
            fontKepala.setFontName("Calibri");
            fontKepala.setFontHeightInPoints((short) 11);
            fontKepala.setBold(true);
            fontKepala.setColor(IndexedColors.WHITE.getIndex());

            XSSFFont fontIsi = workbook.createFont();
            fontIsi.setFontName("Calibri");
            fontIsi.setFontHeightInPoints((short) 11);

            judul = workbook.createCellStyle();
            judul.setFont(fontJudul);
            judul.setAlignment(HorizontalAlignment.CENTER);
            judul.setVerticalAlignment(VerticalAlignment.CENTER);
            judul.setFillForegroundColor(emas);
            judul.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            subjudul = workbook.createCellStyle();
            subjudul.setFont(fontIsi);
            subjudul.setAlignment(HorizontalAlignment.CENTER);

            kepala = workbook.createCellStyle();
            kepala.setFont(fontKepala);
            kepala.setFillForegroundColor(emas);
            kepala.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            garis(kepala);

            seksi = workbook.createCellStyle();
            seksi.setFont(fontTebal);
            seksi.setFillForegroundColor(abu);
            seksi.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            garis(seksi);

            sel = workbook.createCellStyle();
            sel.setFont(fontIsi);
            garis(sel);

            uang = workbook.createCellStyle();
            uang.setFont(fontIsi);
            uang.setDataFormat(format);
            uang.setAlignment(HorizontalAlignment.RIGHT);
            garis(uang);

            selSubtotal = workbook.createCellStyle();
            selSubtotal.setFont(fontTebal);
            garis(selSubtotal);

            uangSubtotal = workbook.createCellStyle();
            uangSubtotal.setFont(fontTebal);
            uangSubtotal.setDataFormat(format);
            uangSubtotal.setAlignment(HorizontalAlignment.RIGHT);
            garis(uangSubtotal);

            selTotal = workbook.createCellStyle();
            selTotal.setFont(fontKepala);
            selTotal.setFillForegroundColor(emas);
            selTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            garis(selTotal);

            uangTotal = workbook.createCellStyle();
            uangTotal.setFont(fontKepala);
            uangTotal.setFillForegroundColor(emas);
            uangTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            uangTotal.setDataFormat(format);
            uangTotal.setAlignment(HorizontalAlignment.RIGHT);
            garis(uangTotal);

            catatan = workbook.createCellStyle();
            catatan.setFont(fontIsi);
            catatan.setWrapText(true);
        }

        private void garis(XSSFCellStyle gaya) {
            gaya.setBorderTop(BorderStyle.THIN);
            gaya.setBorderBottom(BorderStyle.THIN);
            gaya.setBorderLeft(BorderStyle.THIN);
            gaya.setBorderRight(BorderStyle.THIN);
        }
    }
}
