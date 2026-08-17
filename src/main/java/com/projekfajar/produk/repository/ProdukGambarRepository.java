package com.projekfajar.produk.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.produk.model.ProdukGambar;

public interface ProdukGambarRepository extends JpaRepository<ProdukGambar, Long> {

    List<ProdukGambar> findByProdukIdOrderByUrutanAscIdAsc(Long produkId);

    List<ProdukGambar> findByProdukIdIn(Collection<Long> produkIds);

    void deleteByProdukId(Long produkId);
}
