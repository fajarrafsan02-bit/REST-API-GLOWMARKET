package com.projekfajar.ongkir.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.ongkir.model.Ongkir;

public interface OngkirRepository extends JpaRepository<Ongkir, Long> {

    Optional<Ongkir> findByProvinsi(String provinsi);

    List<Ongkir> findAllByOrderByProvinsiAsc();
}