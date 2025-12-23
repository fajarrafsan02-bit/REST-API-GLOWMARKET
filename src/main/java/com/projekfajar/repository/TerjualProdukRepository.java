package com.projekfajar.repository;

import com.projekfajar.models.TerjualProduk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerjualProdukRepository extends JpaRepository<TerjualProduk, Long> {
    
    Optional<TerjualProduk> findByProdukId(Long produkId);
    
    boolean existsByProdukId(Long produkId);
}
