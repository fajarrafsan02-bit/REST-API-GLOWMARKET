package com.projekfajar.repository;

import com.projekfajar.models.OrderStatus;
import com.projekfajar.models.Pesanan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PesananRepository extends JpaRepository<Pesanan, Long> {
    
    Optional<Pesanan> findByNomorPesanan(String nomorPesanan);
    
    List<Pesanan> findByUserId(Long userId);
    
    List<Pesanan> findByStatus(OrderStatus status);
    
    List<Pesanan> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    Optional<Pesanan> findByPaymentId(Long paymentId);
    
    boolean existsByPaymentId(Long paymentId);
    
    Long countByUserId(Long userId);
    
    // Count pesanan by period (all status)
    @Query("SELECT COUNT(p) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Count pesanan by period and status
    @Query("SELECT COUNT(p) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.status = :status")
    Long countByCreatedAtBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate, OrderStatus status);
    
    // Sum total penjualan by period (SELESAI status only)
    @Query("SELECT COALESCE(SUM(p.totalHarga), 0.0) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.status = com.projekfajar.models.OrderStatus.SELESAI")
    Double sumTotalHargaByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    // Sum total penjualan by period (ALL status)
    @Query("SELECT COALESCE(SUM(p.totalHarga), 0.0) FROM Pesanan p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Double sumTotalHargaByPeriodAllStatus(LocalDateTime startDate, LocalDateTime endDate);
    
    // Get daily aggregated data grouped by date (for report)
    @Query("SELECT CAST(p.createdAt AS LocalDate) as tanggal, COUNT(p) as pesanan, " +
           "COALESCE(SUM(p.totalHarga), 0.0) as penjualan " +
           "FROM Pesanan p " +
           "WHERE CAST(p.createdAt AS LocalDate) BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(p.createdAt AS LocalDate) " +
           "ORDER BY CAST(p.createdAt AS LocalDate) ASC")
    List<Object[]> findDailyReportByPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate);
}
