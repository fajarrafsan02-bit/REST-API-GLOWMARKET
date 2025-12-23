package com.projekfajar.controllers;

import com.projekfajar.DTO.GrafikPenjualanBulananResponse;
import com.projekfajar.DTO.GrafikPenjualan12BulanResponse;
import com.projekfajar.DTO.LaporanHarianResponse;
import com.projekfajar.DTO.StatistikPenjualanResponse;
import com.projekfajar.DTO.TotalProdukTerjualResponse;
import com.projekfajar.DTO.TotalPesananResponse;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.StatistikService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistik")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StatistikController {
    private static final Logger logger = LoggerFactory.getLogger(StatistikController.class);

    private final StatistikService statistikService;
    private final UserRepository userRepository;

    @GetMapping("/penjualan/bulan-ini")
    public ResponseEntity<Map<String, Object>> getStatistikPenjualanBulanIni(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            StatistikPenjualanResponse statistik = statistikService.getStatistikBulanIni();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Statistik penjualan bulan ini berhasil diambil",
                    "data", statistik));
        } catch (Exception e) {
            logger.error("Error getting sales statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/produk-terjual/bulan-ini")
    public ResponseEntity<Map<String, Object>> getTotalProdukTerjual(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            TotalProdukTerjualResponse statistik = statistikService.getTotalProdukTerjual();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Total produk terjual bulan ini berhasil diambil",
                    "data", statistik));
        } catch (Exception e) {
            logger.error("Error getting product statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/pesanan/bulan-ini")
    public ResponseEntity<Map<String, Object>> getTotalPesanan(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            TotalPesananResponse statistik = statistikService.getTotalPesanan();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Total pesanan bulan ini berhasil diambil",
                    "data", statistik));
        } catch (Exception e) {
            logger.error("Error getting order statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/grafik/bulanan")
    public ResponseEntity<Map<String, Object>> getGrafikPenjualanBulanan(
            @RequestParam(required = false) Integer tahun,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            List<GrafikPenjualanBulananResponse> grafik = statistikService.getGrafikPenjualanBulanan(tahun);
            System.out.println("INI : " + grafik);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grafik penjualan bulanan berhasil diambil",
                    "data", grafik));
        } catch (Exception e) {
            logger.error("Error getting monthly sales chart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/grafik/12-bulan-terakhir")
    public ResponseEntity<Map<String, Object>> getGrafik12BulanTerakhir(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            List<GrafikPenjualan12BulanResponse> grafik = statistikService.getGrafik12BulanTerakhir();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grafik penjualan 12 bulan terakhir berhasil diambil",
                    "data", grafik));
        } catch (Exception e) {
            logger.error("Error getting 12 months chart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/grafik/tahunan")
    public ResponseEntity<Map<String, Object>> getGrafikPenjualanTahunan(
            @RequestParam(required = false) Integer tahun,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            // If tahun is null, use current year (2025)
            if (tahun == null) {
                tahun = java.time.LocalDateTime.now().getYear();
            }

            List<GrafikPenjualanBulananResponse> grafik = statistikService.getGrafikPenjualanTahunan(tahun);
            System.out.println("INI : " + grafik);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Grafik penjualan tahunan " + tahun + " berhasil diambil",
                    "data", grafik,
                    "tahun", tahun));
        } catch (Exception e) {
            logger.error("Error getting yearly sales chart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/grafik/tahunan/export-excel")
    public ResponseEntity<?> exportGrafikTahunanToExcel(
            @RequestParam(required = false) Integer tahun,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses statistik"));
            }

            // If tahun is null, use current year (2025)
            if (tahun == null) {
                tahun = java.time.LocalDateTime.now().getYear();
            }

            // Generate Excel file
            byte[] excelBytes = statistikService.exportGrafikTahunanToExcel(tahun);

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Grafik_Penjualan_Tahunan_" + tahun + ".xlsx");
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            logger.error("Error exporting yearly sales chart to Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Gagal export ke Excel: " + e.getMessage()));
        }
    }
    
    @GetMapping("/laporan-harian")
    public ResponseEntity<Map<String, Object>> getLaporanHarian(
            @RequestParam(required = true) String startDate,
            @RequestParam(required = true) String endDate,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Only admin can access statistics
            if (user.getRole() != com.projekfajar.models.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Hanya admin yang dapat mengakses laporan"));
            }
            
            // Parse dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            List<LaporanHarianResponse> laporan = statistikService.getLaporanHarian(start, end);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Laporan harian berhasil diambil",
                    "data", laporan,
                    "periode", Map.of(
                            "startDate", startDate,
                            "endDate", endDate
                    )));
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Format tanggal tidak valid. Gunakan format YYYY-MM-DD"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting daily report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Terjadi kesalahan saat mengambil laporan harian"));
        }
    }
}
