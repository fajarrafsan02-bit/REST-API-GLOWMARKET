package com.projekfajar.repository;

import com.projekfajar.models.ProdukTerjual;
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
    
    @Query("SELECT SUM(pt.total) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate")
    Double getTotalPenjualanByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(pt.total) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate AND pt.isSuccess = true")
    Double getTotalPenjualanSuccessByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(pt.qty) FROM ProdukTerjual pt WHERE pt.produk.id = :produkId")
    Integer getTotalTerjualByProduk(Long produkId);
    
    // Count distinct products sold in period
    @Query("SELECT COUNT(DISTINCT pt.produk.id) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate")
    Long countDistinctProdukByPeriod(LocalDateTime startDate, LocalDateTime endDate);
    
    // Count distinct orders (pesanan) in period
    @Query("SELECT COUNT(DISTINCT pt.pesanan.id) FROM ProdukTerjual pt WHERE pt.tanggalBeli BETWEEN :startDate AND :endDate")
    Long countDistinctPesananByPeriod(LocalDateTime startDate, LocalDateTime endDate);
}
