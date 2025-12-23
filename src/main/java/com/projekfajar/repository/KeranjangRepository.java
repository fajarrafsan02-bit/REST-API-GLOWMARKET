package com.projekfajar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.models.Keranjang;

@Repository
public interface KeranjangRepository extends JpaRepository<Keranjang, Long> {
    List<Keranjang> findByUserId(Long userId);
    Optional<Keranjang> findByUserIdAndProdukId(Long userId, Long produkId);
    void deleteByUserId(Long userId);
}
