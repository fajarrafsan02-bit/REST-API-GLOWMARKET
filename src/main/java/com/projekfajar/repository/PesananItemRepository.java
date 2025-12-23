package com.projekfajar.repository;

import com.projekfajar.models.OrderStatus;
import com.projekfajar.models.PesananItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PesananItemRepository extends JpaRepository<PesananItem, Long> {

        // Sum total quantity from pesanan_item for current period (SELESAI status)
        @Query("SELECT COALESCE(SUM(pi.quantity), 0) FROM PesananItem pi " +
                        "WHERE pi.pesanan.createdAt BETWEEN :startDate AND :endDate " +
                        "AND pi.pesanan.status = com.projekfajar.models.OrderStatus.SELESAI")
        Long sumQuantityByPeriod(LocalDateTime startDate, LocalDateTime endDate);

        // Sum all quantity regardless of status (for comparison/testing)
        @Query("SELECT COALESCE(SUM(pi.quantity), 0) FROM PesananItem pi " +
                        "WHERE pi.pesanan.createdAt BETWEEN :startDate AND :endDate")
        Long sumQuantityByPeriodAllStatus(LocalDateTime startDate, LocalDateTime endDate);

        // Get daily product sold grouped by date
        @Query("SELECT CAST(pi.pesanan.createdAt AS LocalDate) as tanggal, " +
                        "COALESCE(SUM(pi.quantity), 0) as totalQuantity " +
                        "FROM PesananItem pi " +
                        "WHERE CAST(pi.pesanan.createdAt AS LocalDate) BETWEEN :startDate AND :endDate " +
                        "GROUP BY CAST(pi.pesanan.createdAt AS LocalDate) " +
                        "ORDER BY CAST(pi.pesanan.createdAt AS LocalDate) ASC")
        java.util.List<Object[]> findDailyProductSoldByPeriod(java.time.LocalDate startDate,
                        java.time.LocalDate endDate);
}
