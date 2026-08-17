package com.projekfajar.poin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.poin.model.RiwayatPoin;

public interface RiwayatPoinRepository extends JpaRepository<RiwayatPoin, Long> {

    List<RiwayatPoin> findByUserIdOrderByCreatedAtDesc(Long userId);
}
