package com.projekfajar.akuntansi.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.projekfajar.akuntansi.model.Jurnal;
import com.projekfajar.akuntansi.model.SumberJurnal;

@Repository
public interface JurnalRepository extends JpaRepository<Jurnal, Long> {

    /** Detail selalu ikut dibaca saat menampilkan jurnal, jadi diambil sekali jalan. */
    @EntityGraph(attributePaths = { "details", "details.akun" })
    List<Jurnal> findByTanggalBetweenOrderByTanggalAscIdAsc(LocalDate mulai, LocalDate sampai);

    boolean existsBySumber(SumberJurnal sumber);

    boolean existsBySumberAndReferensiId(SumberJurnal sumber, Long referensiId);

    /** Dipakai saat membatalkan transaksi: jurnal asal dicari lalu dibuatkan jurnal balik. */
    @EntityGraph(attributePaths = { "details", "details.akun" })
    List<Jurnal> findBySumberAndReferensiIdOrderByIdAsc(SumberJurnal sumber, Long referensiId);

    @EntityGraph(attributePaths = { "details", "details.akun" })
    List<Jurnal> findBySumberAndTanggalBetweenOrderByTanggalAscIdAsc(
            SumberJurnal sumber, LocalDate mulai, LocalDate sampai);

    /** Dipakai memberi nomor urut jurnal dalam satu hari. */
    @Query("SELECT COUNT(j) FROM Jurnal j WHERE j.tanggal = :tanggal")
    long countByTanggal(LocalDate tanggal);

    /**
     * Penjualan yang jurnal HPP-nya tidak terbentuk karena harga modal produknya
     * masih nol. Satu penjualan yang lengkap selalu punya dua jurnal — penjualan
     * dan HPP — jadi yang hanya punya satu berarti labanya belum bisa dihitung.
     */
    @Query("SELECT j.referensiId FROM Jurnal j "
            + "WHERE j.sumber = com.projekfajar.akuntansi.model.SumberJurnal.PENJUALAN "
            + "AND j.tanggal BETWEEN :mulai AND :sampai "
            + "GROUP BY j.referensiId HAVING COUNT(j) < 2")
    List<Long> penjualanTanpaJurnalHpp(LocalDate mulai, LocalDate sampai);
}
