package com.projekfajar.pengembalian.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.pengembalian.model.Pengembalian;
import com.projekfajar.pengembalian.model.PengembalianStatus;

public interface PengembalianRepository extends JpaRepository<Pengembalian, Long> {

    List<Pengembalian> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Pengembalian> findByStatusOrderByCreatedAtDesc(PengembalianStatus status);

    Optional<Pengembalian> findByPesananId(Long pesananId);

    boolean existsByPesananIdAndStatusIn(Long pesananId, List<PengembalianStatus> statuses);
}
