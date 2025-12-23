package com.projekfajar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.models.Produk;
import com.projekfajar.models.StatusProduk;

@Repository
public interface ProdukRepository extends JpaRepository<Produk, Long> {
    List<Produk> findByStatus(StatusProduk status);
    List<Produk> findByNamaContainingIgnoreCase(String nama);
    List<Produk> findByStockLessThan(Integer stock);
}
