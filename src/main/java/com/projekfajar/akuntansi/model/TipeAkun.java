package com.projekfajar.akuntansi.model;

/**
 * Golongan akun dalam bagan akun.
 *
 * HPP dipisahkan dari BEBAN karena laporan laba rugi membedakannya:
 * Pendapatan − HPP = laba kotor, lalu − BEBAN = laba bersih.
 */
public enum TipeAkun {
    ASET,
    LIABILITAS,
    EKUITAS,
    PENDAPATAN,
    HPP,
    BEBAN;

    /** Akun neraca (posisi keuangan), bukan akun laba rugi. */
    public boolean akunNeraca() {
        return this == ASET || this == LIABILITAS || this == EKUITAS;
    }
}
