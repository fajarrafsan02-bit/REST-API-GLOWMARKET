package com.projekfajar.tracking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.tracking.model.TrackingPengiriman;
import com.projekfajar.tracking.model.TrackingStatus;

public interface TrackingPengirimanRepository extends JpaRepository<TrackingPengiriman, Long> {

    List<TrackingPengiriman> findByPesananIdOrderByIdAsc(Long pesananId);

    Optional<TrackingPengiriman> findTopByPesananIdOrderByIdDesc(Long pesananId);

    void deleteByPesananId(Long pesananId);
}
