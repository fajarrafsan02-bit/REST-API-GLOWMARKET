package com.projekfajar.restock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.restock.model.RestockNotifikasi;

public interface RestockNotifikasiRepository extends JpaRepository<RestockNotifikasi, Long> {

    List<RestockNotifikasi> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<RestockNotifikasi> findByUserIdAndProdukIdAndVarianIsNull(Long userId, Long produkId);

    Optional<RestockNotifikasi> findByUserIdAndProdukIdAndVarianId(Long userId, Long produkId, Long varianId);

    List<RestockNotifikasi> findByProdukIdAndAktifTrue(Long produkId);

    List<RestockNotifikasi> findByVarianIdAndAktifTrue(Long varianId);

    List<RestockNotifikasi> findByProdukIdAndVarianIsNullAndAktifTrue(Long produkId);

    boolean existsByUserIdAndProdukIdAndVarianIsNullAndAktifTrue(Long userId, Long produkId);

    boolean existsByUserIdAndProdukIdAndVarianIdAndAktifTrue(Long userId, Long produkId, Long varianId);
}
