package com.projekfajar.akuntansi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.akuntansi.model.Akun;
import com.projekfajar.akuntansi.model.TipeAkun;

@Repository
public interface AkunRepository extends JpaRepository<Akun, Long> {

    Optional<Akun> findByKode(String kode);

    List<Akun> findByAktifTrueOrderByKodeAsc();

    List<Akun> findByTipeAndAktifTrueOrderByKodeAsc(TipeAkun tipe);
}
