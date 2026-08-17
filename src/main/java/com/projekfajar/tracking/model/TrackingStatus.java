package com.projekfajar.tracking.model;

/** Tahapan perjalanan paket dalam timeline tracking. */
public enum TrackingStatus {
    /** Paket disortir di gudang pengirim. */
    DIPROSES,
    /** Paket dalam perjalanan menuju kota tujuan. */
    DALAM_PERJALANAN,
    /** Paket tiba di kantor kurir kota tujuan. */
    SAMPAI_KOTA_TUJUAN,
    /** Kurir sedang mengantar paket ke alamat penerima. */
    OUT_FOR_DELIVERY,
    /** Paket sudah diterima penerima. */
    DITERIMA
}
