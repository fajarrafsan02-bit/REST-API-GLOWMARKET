package com.projekfajar.repository;

import com.projekfajar.models.Alamat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlamatRepository extends JpaRepository<Alamat, Long> {
    List<Alamat> findByUserId(Long userId);
    Optional<Alamat> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<Alamat> findByIdAndUserId(Long id, Long userId);
}
