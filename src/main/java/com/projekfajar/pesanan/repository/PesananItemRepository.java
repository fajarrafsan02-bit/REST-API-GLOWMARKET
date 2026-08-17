package com.projekfajar.pesanan.repository;

import com.projekfajar.pesanan.model.PesananItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PesananItemRepository extends JpaRepository<PesananItem, Long> {

        // Sum total quantity from pesanan_item for current period (SELESAI status)
        @Query("SELECT COALESCE(SUM(pi.quantity), 0) FROM PesananItem pi " +
                        "WHERE pi.pesanan.createdAt BETWEEN :startDate AND :endDate " +
                        "AND pi.pesanan.status = com.projekfajar.pesanan.model.OrderStatus.SELESAI")
        Long sumQuantityByPeriod(LocalDateTime startDate, LocalDateTime endDate);

        // Sum quantity penjualan bersih (bukan pending, batal, atau retur).
        @Query("SELECT COALESCE(SUM(pi.quantity), 0) FROM PesananItem pi " +
                        "WHERE pi.pesanan.createdAt BETWEEN :startDate AND :endDate " +
                        "AND pi.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, " +
                        "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, " +
                        "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN)")
        Long sumQuantityByPeriodAllStatus(LocalDateTime startDate, LocalDateTime endDate);

        boolean existsByGambarProduk(String gambarProduk);

        // Get daily product sold grouped by date
        @Query("SELECT CAST(pi.pesanan.createdAt AS LocalDate) as tanggal, " +
                        "COALESCE(SUM(pi.quantity), 0) as totalQuantity " +
                        "FROM PesananItem pi " +
                        "WHERE CAST(pi.pesanan.createdAt AS LocalDate) BETWEEN :startDate AND :endDate " +
                        "AND pi.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, " +
                        "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, " +
                        "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN) " +
                        "GROUP BY CAST(pi.pesanan.createdAt AS LocalDate) " +
                        "ORDER BY CAST(pi.pesanan.createdAt AS LocalDate) ASC")
        java.util.List<Object[]> findDailyProductSoldByPeriod(java.time.LocalDate startDate,
                        java.time.LocalDate endDate);
}
