package com.projekfajar.produk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.StatusProduk;

@Repository
public interface ProdukRepository extends JpaRepository<Produk, Long> {
    List<Produk> findByStatus(StatusProduk status);
    List<Produk> findByNamaContainingIgnoreCase(String nama);
    List<Produk> findByStockLessThan(Integer stock);

    /*
     * Varian "deletedFalse" dipakai untuk katalog, admin, dan keranjang.
     * Query tanpa filter tetap ada agar riwayat pesanan/review yang
     * menunjuk produk terhapus masih bisa dibaca.
     */
    List<Produk> findByDeletedFalse();

    Optional<Produk> findByIdAndDeletedFalse(Long id);

    List<Produk> findByStatusAndDeletedFalse(StatusProduk status);

    List<Produk> findByNamaContainingIgnoreCaseAndDeletedFalse(String nama);
}
