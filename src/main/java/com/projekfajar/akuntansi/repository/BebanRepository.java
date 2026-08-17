package com.projekfajar.akuntansi.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.akuntansi.model.Beban;

@Repository
public interface BebanRepository extends JpaRepository<Beban, Long> {

    @EntityGraph(attributePaths = { "akun" })
    List<Beban> findByTanggalBetweenOrderByTanggalDescIdDesc(LocalDate mulai, LocalDate sampai);
}
