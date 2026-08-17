package com.projekfajar.pesanan.repository;

import java.math.BigDecimal;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PesananRepository extends JpaRepository<Pesanan, Long> {
    
    /*
     * Pemetaan pesanan selalu membaca item beserta produknya. Tanpa entity
     * graph, daftar berisi N pesanan memicu query tambahan per pesanan dan per
     * produk — N+1 klasik. Graph ini mengambil semuanya sekali jalan.
     */
    Optional<Pesanan> findByNomorPesanan(String nomorPesanan);

    @Override
    @EntityGraph(attributePaths = { "items", "items.produk", "payment", "user", "alamat" })
    List<Pesanan> findAll();

    @EntityGraph(attributePaths = { "items", "items.produk", "payment", "user", "alamat" })
    List<Pesanan> findByUserId(Long userId);

    /** Dipakai pengiriman email yang berjalan di luar transaksi aslinya. */
    @EntityGraph(attributePaths = { "items", "items.produk", "payment", "user" })
    Optional<Pesanan> findWithItemsById(Long id);
    
    List<Pesanan> findByStatus(OrderStatus status);
    
    List<Pesanan> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    Optional<Pesanan> findByPaymentId(Long paymentId);

    Optional<Pesanan> findByPaymentExternalId(String externalId);
    
    boolean existsByPaymentId(Long paymentId);
    
    Long countByUserId(Long userId);
    
    // Penjualan bersih: sudah dibayar, belum dibatalkan, belum di-refund.
    String STATUS_TERBAYAR = "p.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN)";

    @Query("SELECT COUNT(p) FROM Pesanan p WHERE " + STATUS_TERBAYAR)
    Long countPesananTerbayar();

    // Count pesanan by period (semua status terbayar)
    @Query("SELECT COUNT(p) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate AND " + STATUS_TERBAYAR)
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Count pesanan by period and status
    @Query("SELECT COUNT(p) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.status = :status")
    Long countByCreatedAtBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
    
    // Sum total penjualan by period (SELESAI status only)
    @Query("SELECT COALESCE(SUM(p.totalHarga), 0) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.status = com.projekfajar.pesanan.model.OrderStatus.SELESAI")
    BigDecimal sumTotalHargaByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    // Sum total penjualan by period (semua status terbayar)
    @Query("SELECT COALESCE(SUM(p.totalHarga), 0) FROM Pesanan p "
            + "WHERE p.createdAt BETWEEN :startDate AND :endDate AND " + STATUS_TERBAYAR)
    BigDecimal sumTotalHargaByPeriodAllStatus(LocalDateTime startDate, LocalDateTime endDate);
    
    // Get daily aggregated data grouped by date (for report)
    @Query("SELECT CAST(p.createdAt AS LocalDate) as tanggal, COUNT(p) as pesanan, " +
           "COALESCE(SUM(p.totalHarga), 0) as penjualan " +
           "FROM Pesanan p " +
           "WHERE CAST(p.createdAt AS LocalDate) BETWEEN :startDate AND :endDate " +
           "AND p.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, " +
           "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, " +
           "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN) " +
           "GROUP BY CAST(p.createdAt AS LocalDate) " +
           "ORDER BY CAST(p.createdAt AS LocalDate) ASC")
    List<Object[]> findDailyReportByPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
