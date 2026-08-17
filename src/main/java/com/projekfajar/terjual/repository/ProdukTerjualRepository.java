package com.projekfajar.terjual.repository;

import java.math.BigDecimal;
import com.projekfajar.terjual.model.ProdukTerjual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProdukTerjualRepository extends JpaRepository<ProdukTerjual, Long> {
    
    List<ProdukTerjual> findByUserId(Long userId);
    
    List<ProdukTerjual> findByProdukId(Long produkId);
    
    List<ProdukTerjual> findByPesananId(Long pesananId);
    
    @Query("SELECT pt FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate")
    List<ProdukTerjual> findByTanggalBeliBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(pt) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate")
    Long countByTanggalBeliBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(pt) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate AND pt.isSuccess = true")
    Long countSuccessByTanggalBeliBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(pt.total) FROM ProdukTerjual pt "
            + "WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate "
            + "AND pt.isSuccess = true "
            + "AND pt.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN)")
    BigDecimal getTotalPenjualanByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(pt.total) FROM ProdukTerjual pt "
            + "WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate "
            + "AND pt.isSuccess = true "
            + "AND pt.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN)")
    BigDecimal getTotalPenjualanSuccessByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(pt.qty), 0) FROM ProdukTerjual pt "
            + "WHERE pt.produk.id = :produkId AND pt.isSuccess = true "
            + "AND pt.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN)")
    Integer getTotalTerjualByProduk(Long produkId);

    /**
     * Jumlah terjual per produk, dihitung langsung dari catatan transaksi.
     * Menggantikan tabel counter terpisah yang dulu bisa berbeda dari data asli.
     */
    @Query("SELECT pt.produk.id, COALESCE(SUM(pt.qty), 0) FROM ProdukTerjual pt "
            + "WHERE pt.isSuccess = true "
            + "AND pt.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN) "
            + "GROUP BY pt.produk.id")
    List<Object[]> sumQtyGroupByProduk();
    
    // Count distinct products sold in period
    @Query("SELECT COUNT(DISTINCT pt.produk.id) FROM ProdukTerjual pt "
            + "WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate "
            + "AND pt.isSuccess = true "
            + "AND pt.pesanan.status NOT IN (com.projekfajar.pesanan.model.OrderStatus.PENDING, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIBATALKAN, "
            + "com.projekfajar.pesanan.model.OrderStatus.DIKEMBALIKAN)")
    Long countDistinctProdukByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    // Count distinct orders (pesanan) in period
    @Query("SELECT COUNT(DISTINCT pt.pesanan.id) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate")
    Long countDistinctPesananByPeriod(LocalDateTime startDate, LocalDateTime endDate);
}
