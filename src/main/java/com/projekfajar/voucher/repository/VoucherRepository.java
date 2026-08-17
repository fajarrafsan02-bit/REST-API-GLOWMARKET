package com.projekfajar.voucher.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projekfajar.voucher.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByKode(String kode);

    List<Voucher> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Voucher umum (tanpa pemilik) yang sedang aktif dan bisa dipakai siapa
     * saja — ditampilkan ke pembeli di halaman Poin Loyalitas sebagai
     * "Voucher Tersedia".
     */
    @Query("SELECT v FROM Voucher v WHERE v.user IS NULL AND v.aktif = true "
            + "AND (v.berlakuDari IS NULL OR v.berlakuDari <= :now) "
            + "AND (v.berlakuSampai IS NULL OR v.berlakuSampai >= :now) "
            + "AND (v.kuota IS NULL OR v.terpakai < v.kuota) "
            + "ORDER BY v.createdAt DESC")
    List<Voucher> findPublicAktif(@Param("now") LocalDateTime now);
}
