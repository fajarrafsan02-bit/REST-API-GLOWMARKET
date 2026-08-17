package com.projekfajar.akuntansi.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.akuntansi.model.Pembelian;

@Repository
public interface PembelianRepository extends JpaRepository<Pembelian, Long> {

    /** Daftar pembelian selalu ditampilkan bersama itemnya, jadi diambil sekali jalan. */
    @EntityGraph(attributePaths = { "items", "items.produk" })
    List<Pembelian> findByTanggalBetweenOrderByTanggalDescIdDesc(LocalDate mulai, LocalDate sampai);

    @EntityGraph(attributePaths = { "items", "items.produk" })
    Optional<Pembelian> findWithItemsById(Long id);
}
