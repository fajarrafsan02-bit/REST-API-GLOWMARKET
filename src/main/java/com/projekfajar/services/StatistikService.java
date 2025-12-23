package com.projekfajar.services;

import com.projekfajar.DTO.GrafikPenjualanBulananResponse;
import com.projekfajar.DTO.GrafikPenjualan12BulanResponse;
import com.projekfajar.DTO.LaporanHarianResponse;
import com.projekfajar.DTO.StatistikPenjualanResponse;
import com.projekfajar.DTO.TotalProdukTerjualResponse;
import com.projekfajar.DTO.TotalPesananResponse;
import com.projekfajar.repository.ProdukTerjualRepository;
import com.projekfajar.repository.PesananItemRepository;
import com.projekfajar.repository.PesananRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatistikService {
    private static final Logger logger = LoggerFactory.getLogger(StatistikService.class);

    private final ProdukTerjualRepository produkTerjualRepository;
    private final PesananItemRepository pesananItemRepository;
    private final PesananRepository pesananRepository;

    @Transactional(readOnly = true)
    public StatistikPenjualanResponse getStatistikBulanIni() {
        logger.info("Getting sales statistics for current month");

        // Get current month start and end
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get previous month start and end
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime startOfPrevMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfPrevMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get current month data (sum of total_harga from pesanan - ALL status)
        Double totalPenjualan = pesananRepository.sumTotalHargaByPeriodAllStatus(startOfMonth, endOfMonth);

        // Get previous month data
        Double totalPenjualanBulanLalu = pesananRepository.sumTotalHargaByPeriodAllStatus(startOfPrevMonth,
                endOfPrevMonth);

        // Handle null cases
        if (totalPenjualan == null)
            totalPenjualan = 0.0;
        if (totalPenjualanBulanLalu == null)
            totalPenjualanBulanLalu = 0.0;

        // Calculate percentage changes
        Double persenPenjualan = calculatePercentageChange(totalPenjualanBulanLalu, totalPenjualan);

        logger.info("Statistics for month {}/{}: total sales: {} ({}%)",
                currentMonth.getMonthValue(), currentMonth.getYear(),
                totalPenjualan, persenPenjualan);

        return StatistikPenjualanResponse.builder()
                .totalPenjualan(totalPenjualan)
                .bulan(currentMonth.getMonthValue())
                .tahun(currentMonth.getYear())
                .persenPenjualan(persenPenjualan)
                .build();
    }

    @Transactional(readOnly = true)
    public TotalProdukTerjualResponse getTotalProdukTerjual() {
        logger.info("Getting total products sold (sum of quantity) for current month");

        // Get current month start and end
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get previous month start and end
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime startOfPrevMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfPrevMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get current month data (sum of quantity from pesanan_item)
        Long totalJenisProduk = pesananItemRepository.sumQuantityByPeriodAllStatus(startOfMonth, endOfMonth);
        logger.info("Total products sold with SELESAI status: {}", totalJenisProduk);

        // Get previous month data
        Long totalJenisProdukBulanLalu = pesananItemRepository.sumQuantityByPeriodAllStatus(startOfPrevMonth,
                endOfPrevMonth);
        System.out.println("TOTAL JENIS BULAN LALU : " + totalJenisProdukBulanLalu);
        System.out.println("TOTAL JENIS BULAN LALU : " + totalJenisProduk);

        // Handle null cases (should not happen with COALESCE but keep for safety)
        if (totalJenisProduk == null)
            totalJenisProduk = 0L;
        if (totalJenisProdukBulanLalu == null)
            totalJenisProdukBulanLalu = 0L;

        // Calculate percentage changes
        Double persenProduk = calculatePercentageChange(totalJenisProdukBulanLalu, totalJenisProduk);
        System.out.println("PERSENTASE : " + persenProduk);

        logger.info("Total products sold (quantity sum) for month {}/{}: {} ({}%)",
                currentMonth.getMonthValue(), currentMonth.getYear(),
                totalJenisProduk, persenProduk);

        return TotalProdukTerjualResponse.builder()
                .totalJenisProduk(totalJenisProduk)
                .bulan(currentMonth.getMonthValue())
                .tahun(currentMonth.getYear())
                .persenProduk(persenProduk)
                .build();
    }

    @Transactional(readOnly = true)
    public TotalPesananResponse getTotalPesanan() {
        logger.info("Getting total orders for current month");

        // Get current month start and end
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get previous month start and end
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime startOfPrevMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfPrevMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get current month data (count from pesanan table)
        Long totalPesanan = pesananRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

        // Get previous month data
        Long totalPesananBulanLalu = pesananRepository.countByCreatedAtBetween(startOfPrevMonth,
                endOfPrevMonth);

        // Handle null cases
        if (totalPesanan == null)
            totalPesanan = 0L;
        if (totalPesananBulanLalu == null)
            totalPesananBulanLalu = 0L;

        // Calculate percentage changes
        Double persenPesanan = calculatePercentageChange(totalPesananBulanLalu, totalPesanan);

        logger.info("Total orders for month {}/{}: {} ({}%)",
                currentMonth.getMonthValue(), currentMonth.getYear(),
                totalPesanan, persenPesanan);

        return TotalPesananResponse.builder()
                .totalPesanan(totalPesanan)
                .bulan(currentMonth.getMonthValue())
                .tahun(currentMonth.getYear())
                .persenPesanan(persenPesanan)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GrafikPenjualan12BulanResponse> getGrafik12BulanTerakhir() {
        logger.info("Getting sales chart for last 12 months");

        List<GrafikPenjualan12BulanResponse> grafikList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Loop dari 11 bulan lalu sampai bulan ini (total 12 bulan)
        for (int i = 11; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(now).minusMonths(i);
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            // Get total produk terjual (distinct produk)
            Long totalProdukTerjual = produkTerjualRepository.countDistinctProdukByPeriod(startOfMonth, endOfMonth);

            // Get total penjualan (Rp)
            Double totalPenjualan = produkTerjualRepository.getTotalPenjualanByPeriod(startOfMonth, endOfMonth);

            // Handle null
            if (totalProdukTerjual == null)
                totalProdukTerjual = 0L;
            if (totalPenjualan == null)
                totalPenjualan = 0.0;

            // Get month name in Indonesian
            String namaBulan = yearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));

            GrafikPenjualan12BulanResponse grafik = GrafikPenjualan12BulanResponse.builder()
                    .bulan(yearMonth.getMonthValue())
                    .tahun(yearMonth.getYear())
                    .namaBulan(namaBulan)
                    .totalProdukTerjual(totalProdukTerjual)
                    .totalPenjualan(totalPenjualan)
                    .build();

            grafikList.add(grafik);
        }

        logger.info("Generated chart data for last 12 months: {} entries", grafikList.size());
        return grafikList;
    }

    private Double calculatePercentageChange(Long oldValue, Long newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        return ((newValue - oldValue) * 100.0) / oldValue;
    }

    private Double calculatePercentageChange(Double oldValue, Double newValue) {
        if (oldValue == 0.0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        return ((newValue - oldValue) * 100.0) / oldValue;
    }

    @Transactional(readOnly = true)
    public List<GrafikPenjualanBulananResponse> getGrafikPenjualanBulanan(Integer tahun) {
        logger.info("Getting monthly sales chart for year: {}", tahun);

        List<GrafikPenjualanBulananResponse> grafikList = new ArrayList<>();

        // If tahun is null, use current year
        if (tahun == null) {
            tahun = LocalDateTime.now().getYear();
        }

        // Loop through all 12 months
        for (int bulan = 1; bulan <= 12; bulan++) {
            YearMonth yearMonth = YearMonth.of(tahun, bulan);
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            // Get total pesanan from pesanan table (count rows by created_at)
            Long totalPesanan = pesananRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

            // Get total produk terjual from pesanan_item table (sum of quantity)
            Long totalProdukTerjual = pesananItemRepository.sumQuantityByPeriodAllStatus(startOfMonth, endOfMonth);

            // Get total penjualan (sum of total_harga from pesanan - ALL status)
            Double totalPenjualan = pesananRepository.sumTotalHargaByPeriodAllStatus(startOfMonth, endOfMonth);

            // Handle null
            if (totalPesanan == null)
                totalPesanan = 0L;
            if (totalProdukTerjual == null)
                totalProdukTerjual = 0L;
            if (totalPenjualan == null)
                totalPenjualan = 0.0;

            // Get month name in Indonesian
            String namaBulan = yearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));

            GrafikPenjualanBulananResponse grafik = GrafikPenjualanBulananResponse.builder()
                    .bulan(bulan)
                    .tahun(tahun)
                    .namaBulan(namaBulan)
                    .totalPesanan(totalPesanan)
                    .totalProdukTerjual(totalProdukTerjual)
                    .totalPenjualan(totalPenjualan)
                    .build();

            grafikList.add(grafik);
        }

        logger.info("Generated chart data for {} months", grafikList.size());
        return grafikList;
    }

    @Transactional(readOnly = true)
    public List<GrafikPenjualanBulananResponse> getGrafikPenjualanTahunan(Integer tahun) {
        logger.info("Getting yearly sales chart for year: {}", tahun);

        List<GrafikPenjualanBulananResponse> grafikList = new ArrayList<>();

        // If tahun is null, use current year (e.g., 2025)
        if (tahun == null) {
            tahun = LocalDateTime.now().getYear();
        }

        // Loop through all 12 months of the specified year
        for (int bulan = 1; bulan <= 12; bulan++) {
            YearMonth yearMonth = YearMonth.of(tahun, bulan);
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            // Get total pesanan from pesanan table (count rows by created_at)
            Long totalPesanan = pesananRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

            // Get total produk terjual from pesanan_item table (sum of quantity)
            Long totalProdukTerjual = pesananItemRepository.sumQuantityByPeriodAllStatus(startOfMonth, endOfMonth);

            // Get total penjualan (sum of total_harga from pesanan - ALL status)
            Double totalPenjualan = pesananRepository.sumTotalHargaByPeriodAllStatus(startOfMonth, endOfMonth);

            // Handle null
            if (totalPesanan == null)
                totalPesanan = 0L;
            if (totalProdukTerjual == null)
                totalProdukTerjual = 0L;
            if (totalPenjualan == null)
                totalPenjualan = 0.0;

            // Get month name in Indonesian
            String namaBulan = yearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));

            GrafikPenjualanBulananResponse grafik = GrafikPenjualanBulananResponse.builder()
                    .bulan(bulan)
                    .tahun(tahun)
                    .namaBulan(namaBulan)
                    .totalPesanan(totalPesanan)
                    .totalProdukTerjual(totalProdukTerjual)
                    .totalPenjualan(totalPenjualan)
                    .build();

            grafikList.add(grafik);
        }

        logger.info("Generated yearly chart data for {}: {} entries", tahun, grafikList.size());
        return grafikList;
    }

    public byte[] exportGrafikTahunanToExcel(Integer tahun) throws IOException {
        logger.info("Exporting yearly sales chart to Excel for year: {}", tahun);

        // Get data
        List<GrafikPenjualanBulananResponse> grafikList = getGrafikPenjualanTahunan(tahun);

        // Create workbook and sheet
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Laporan Penjualan " + tahun);

        // Fonts
        XSSFFont titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFFont headerFont = workbook.createFont();
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFFont bodyFont = workbook.createFont();
        bodyFont.setFontName("Calibri");
        bodyFont.setFontHeightInPoints((short) 11);

        XSSFFont totalFont = workbook.createFont();
        totalFont.setFontName("Calibri");
        totalFont.setFontHeightInPoints((short) 12);
        totalFont.setBold(true);
        totalFont.setColor(IndexedColors.WHITE.getIndex());

        // Cell Styles
        XSSFCellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(184, 134, 11), null)); // Dark Golden
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(218, 165, 32), null)); // Goldenrod
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle rowStyleEven = workbook.createCellStyle();
        rowStyleEven.setFont(bodyFont);
        rowStyleEven.setFillForegroundColor(new XSSFColor(new java.awt.Color(255, 253, 248), null)); // Very light gold
        rowStyleEven.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        rowStyleEven.setBorderTop(BorderStyle.THIN);
        rowStyleEven.setBorderBottom(BorderStyle.THIN);
        rowStyleEven.setBorderLeft(BorderStyle.THIN);
        rowStyleEven.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle rowStyleOdd = workbook.createCellStyle();
        rowStyleOdd.setFont(bodyFont);
        rowStyleOdd.setBorderTop(BorderStyle.THIN);
        rowStyleOdd.setBorderBottom(BorderStyle.THIN);
        rowStyleOdd.setBorderLeft(BorderStyle.THIN);
        rowStyleOdd.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.cloneStyleFrom(rowStyleOdd);
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("Rp #,##0"));

        XSSFCellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(rowStyleOdd);
        numberStyle.setDataFormat(format.getFormat("#,##0"));
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);

        XSSFCellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(totalFont);
        totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(139, 69, 19), null)); // Saddle Brown
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderTop(BorderStyle.MEDIUM);
        totalStyle.setBorderBottom(BorderStyle.MEDIUM);

        // Title Row
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(40);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("LAPORAN PENJUALAN TAHUNAN " + tahun);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Empty row
        sheet.createRow(1);

        // Header Row
        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(30);
        String[] headers = { "No", "Bulan", "Nama Bulan", "Total Pesanan", "Total Penjualan (Rp)" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowNum = 3;
        long totalPesananSum = 0;
        double totalPenjualanSum = 0.0;

        for (GrafikPenjualanBulananResponse grafik : grafikList) {
            Row row = sheet.createRow(rowNum++);
            row.setHeightInPoints(25);

            XSSFCellStyle currentRowStyle = (rowNum % 2 == 0) ? rowStyleEven : rowStyleOdd;

            // No
            Cell cell0 = row.createCell(0);
            cell0.setCellValue(rowNum - 3);
            cell0.setCellStyle(currentRowStyle);
            cell0.setCellStyle(numberStyle);

            // Bulan Number
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(grafik.getBulan());
            cell1.setCellStyle(currentRowStyle);
            cell1.setCellStyle(numberStyle);

            // Nama Bulan
            Cell cell2 = row.createCell(2);
            cell2.setCellValue(grafik.getNamaBulan());
            cell2.setCellStyle(currentRowStyle);

            // Total Pesanan
            Cell cell3 = row.createCell(3);
            long pesanan = grafik.getTotalPesanan() != null ? grafik.getTotalPesanan() : 0;
            cell3.setCellValue(pesanan);
            cell3.setCellStyle(currentRowStyle);
            cell3.setCellStyle(numberStyle);
            totalPesananSum += pesanan;

            // Total Penjualan
            Cell cell4 = row.createCell(4);
            double penjualan = grafik.getTotalPenjualan() != null ? grafik.getTotalPenjualan() : 0.0;
            cell4.setCellValue(penjualan);
            XSSFCellStyle currCurrency = workbook.createCellStyle();
            currCurrency.cloneStyleFrom(currentRowStyle);
            currCurrency.setDataFormat(format.getFormat("Rp #,##0"));
            currCurrency.setAlignment(HorizontalAlignment.RIGHT);
            currCurrency.setBorderTop(BorderStyle.THIN);
            currCurrency.setBorderBottom(BorderStyle.THIN);
            currCurrency.setBorderLeft(BorderStyle.THIN);
            currCurrency.setBorderRight(BorderStyle.THIN);
            cell4.setCellStyle(currCurrency);
            totalPenjualanSum += penjualan;
        }

        // Total Row
        Row totalRow = sheet.createRow(rowNum);
        totalRow.setHeightInPoints(35);

        Cell totalLabel = totalRow.createCell(2);
        totalLabel.setCellValue("TOTAL TAHUN " + tahun);
        totalLabel.setCellStyle(totalStyle);

        Cell totalPesananCell = totalRow.createCell(3);
        totalPesananCell.setCellValue(totalPesananSum);
        totalPesananCell.setCellStyle(totalStyle);
        totalPesananCell.setCellStyle(numberStyle);

        Cell totalPenjualanCell = totalRow.createCell(4);
        totalPenjualanCell.setCellValue(totalPenjualanSum);
        XSSFCellStyle totalCurrency = workbook.createCellStyle();
        totalCurrency.cloneStyleFrom(totalStyle);
        totalCurrency.setDataFormat(format.getFormat("Rp #,##0"));
        totalCurrency.setAlignment(HorizontalAlignment.RIGHT);
        totalPenjualanCell.setCellStyle(totalCurrency);

        // Auto-size columns with padding
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, width + 1500); // Tambah padding
        }

        // Tambah border luar tabel
        for (int i = 2; i <= rowNum; i++) {
            Row r = sheet.getRow(i);
            if (r != null) {
                for (int j = 0; j < 5; j++) {
                    Cell c = r.getCell(j);
                    if (c == null)
                        c = r.createCell(j);
                    XSSFCellStyle borderStyle = workbook.createCellStyle();
                    borderStyle.cloneStyleFrom(c.getCellStyle());
                    borderStyle.setBorderTop(BorderStyle.THIN);
                    borderStyle.setBorderBottom(BorderStyle.THIN);
                    borderStyle.setBorderLeft(BorderStyle.THIN);
                    borderStyle.setBorderRight(BorderStyle.THIN);
                    c.setCellStyle(borderStyle);
                }
            }
        }

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        logger.info("Excel export completed for year {}", tahun);
        return outputStream.toByteArray();
    }
    
    @Transactional(readOnly = true)
    public List<LaporanHarianResponse> getLaporanHarian(LocalDate startDate, LocalDate endDate) {
        logger.info("Getting daily report from {} to {}", startDate, endDate);
        
        // Validate dates
        if (startDate == null || endDate == null) {
            logger.error("Start date and end date cannot be null");
            throw new IllegalArgumentException("Tanggal mulai dan tanggal akhir tidak boleh kosong");
        }
        
        if (startDate.isAfter(endDate)) {
            logger.error("Start date cannot be after end date");
            throw new IllegalArgumentException("Tanggal mulai tidak boleh lebih besar dari tanggal akhir");
        }
        
        // Get daily report data from pesanan
        List<Object[]> pesananData = pesananRepository.findDailyReportByPeriod(startDate, endDate);
        
        // Get daily product sold data
        List<Object[]> produkData = pesananItemRepository.findDailyProductSoldByPeriod(startDate, endDate);
        
        // Create a map for product data for easier lookup
        Map<LocalDate, Long> produkMap = new HashMap<>();
        for (Object[] row : produkData) {
            LocalDate tanggal = (LocalDate) row[0];
            Long quantity = ((Number) row[1]).longValue();
            produkMap.put(tanggal, quantity);
        }
        
        // Build response list
        List<LaporanHarianResponse> laporanList = new ArrayList<>();
        
        for (Object[] row : pesananData) {
            LocalDate tanggal = (LocalDate) row[0];
            Long pesanan = ((Number) row[1]).longValue();
            Double penjualan = ((Number) row[2]).doubleValue();
            Long produkTerjual = produkMap.getOrDefault(tanggal, 0L);
            
            LaporanHarianResponse laporan = LaporanHarianResponse.builder()
                    .tanggal(tanggal)
                    .pesanan(pesanan)
                    .penjualan(penjualan)
                    .produkTerjual(produkTerjual)
                    .build();
            
            laporanList.add(laporan);
        }
        
        logger.info("Generated daily report: {} entries", laporanList.size());
        return laporanList;
    }
}
