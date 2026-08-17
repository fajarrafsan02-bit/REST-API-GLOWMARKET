package com.projekfajar.akuntansi.model;

/** Asal sebuah jurnal, dipakai untuk menelusuri kembali ke transaksinya. */
public enum SumberJurnal {
    PENJUALAN,
    PEMBELIAN,
    BEBAN,
    SALDO_AWAL,
    /** Stok yang diubah admin di luar pembelian dan penjualan. */
    PENYESUAIAN,
    /** Pelunasan utang pembelian kredit: kas keluar, utang turun. */
    PELUNASAN,
    /** Pengembalian uang ke pembeli atas pengajuan pengembalian barang. */
    REFUND,
    MANUAL
}
