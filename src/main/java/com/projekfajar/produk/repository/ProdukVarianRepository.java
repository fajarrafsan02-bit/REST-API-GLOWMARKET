package com.projekfajar.produk.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.produk.model.ProdukVarian;

public interface ProdukVarianRepository extends JpaRepository<ProdukVarian, Long> {

    List<ProdukVarian> findByProdukIdOrderByIdAsc(Long produkId);

    List<ProdukVarian> findByProdukIdAndAktifTrueOrderByIdAsc(Long produkId);

    List<ProdukVarian> findByProdukIdIn(Collection<Long> produkIds);
}
