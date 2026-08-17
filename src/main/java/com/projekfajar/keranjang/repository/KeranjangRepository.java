package com.projekfajar.keranjang.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.keranjang.model.Keranjang;

@Repository
public interface KeranjangRepository extends JpaRepository<Keranjang, Long> {
    List<Keranjang> findByUserId(Long userId);
    Optional<Keranjang> findByUserIdAndProdukId(Long userId, Long produkId);
    Optional<Keranjang> findByUserIdAndProdukIdAndVariantId(Long userId, Long produkId, Long variantId);
    Optional<Keranjang> findByUserIdAndProdukIdAndVariantIsNull(Long userId, Long produkId);
    void deleteByUserId(Long userId);

    /** Dipakai saat produk dihapus, agar tidak tertinggal di keranjang siapa pun. */
    void deleteByProdukId(Long produkId);
}
