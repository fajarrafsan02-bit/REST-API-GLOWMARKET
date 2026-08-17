package com.projekfajar.akuntansi.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.projekfajar.akuntansi.model.JurnalDetail;
import com.projekfajar.akuntansi.model.TipeAkun;

/**
 * Semua saldo laporan dihitung di sini langsung dari jurnal — tidak ada tabel
 * saldo terpisah yang bisa berbeda dari mutasi sebenarnya.
 */
@Repository
public interface JurnalDetailRepository extends JpaRepository<JurnalDetail, Long> {

    /** Mutasi satu akun pada rentang tanggal, untuk buku besar. */
    @Query("SELECT d FROM JurnalDetail d "
            + "JOIN FETCH d.jurnal j "
            + "WHERE d.akun.id = :akunId AND j.tanggal BETWEEN :mulai AND :sampai "
            + "ORDER BY j.tanggal ASC, j.id ASC")
    List<JurnalDetail> findMutasiAkun(Long akunId, LocalDate mulai, LocalDate sampai);

    /** Saldo akun sampai tanggal tertentu, dinyatakan sebagai debit − kredit. */
    @Query("SELECT COALESCE(SUM(d.debit - d.kredit), 0) FROM JurnalDetail d "
            + "WHERE d.akun.id = :akunId AND d.jurnal.tanggal <= :sampai")
    BigDecimal saldoAkunSampai(Long akunId, LocalDate sampai);

    /** Saldo akun sebelum tanggal tertentu — saldo awal pada buku besar. */
    @Query("SELECT COALESCE(SUM(d.debit - d.kredit), 0) FROM JurnalDetail d "
            + "WHERE d.akun.id = :akunId AND d.jurnal.tanggal < :sebelum")
    BigDecimal saldoAkunSebelum(Long akunId, LocalDate sebelum);

    /**
     * Ringkasan per akun untuk satu golongan pada rentang tanggal.
     * Baris: [akunId, kode, nama, debit − kredit]
     */
    @Query("SELECT d.akun.id, d.akun.kode, d.akun.nama, COALESCE(SUM(d.debit - d.kredit), 0) "
            + "FROM JurnalDetail d "
            + "WHERE d.akun.tipe = :tipe AND d.jurnal.tanggal BETWEEN :mulai AND :sampai "
            + "GROUP BY d.akun.id, d.akun.kode, d.akun.nama "
            + "ORDER BY d.akun.kode ASC")
    List<Object[]> ringkasPerAkun(TipeAkun tipe, LocalDate mulai, LocalDate sampai);

    /**
     * Ringkasan per akun sampai tanggal tertentu (akun neraca bersifat kumulatif).
     * Baris: [akunId, kode, nama, debit − kredit]
     */
    @Query("SELECT d.akun.id, d.akun.kode, d.akun.nama, COALESCE(SUM(d.debit - d.kredit), 0) "
            + "FROM JurnalDetail d "
            + "WHERE d.akun.tipe = :tipe AND d.jurnal.tanggal <= :sampai "
            + "GROUP BY d.akun.id, d.akun.kode, d.akun.nama "
            + "ORDER BY d.akun.kode ASC")
    List<Object[]> ringkasPerAkunSampai(TipeAkun tipe, LocalDate sampai);

    /** Penjaga keseimbangan pembukuan: harus selalu nol. */
    @Query("SELECT COALESCE(SUM(d.debit - d.kredit), 0) FROM JurnalDetail d")
    BigDecimal selisihDebitKredit();
}
