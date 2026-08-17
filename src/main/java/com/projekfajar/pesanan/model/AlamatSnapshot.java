package com.projekfajar.pesanan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Salinan alamat pengiriman pada saat pesanan dibuat.
 *
 * Pesanan menyimpan FK ke tabel alamat, tetapi alamat itu masih bisa diedit
 * atau dihapus pemiliknya. Tanpa salinan ini, riwayat pesanan lama ikut
 * berubah ketika pembeli memperbarui alamatnya.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlamatSnapshot {

    @Column(name = "snapshot_nama_lengkap")
    private String namaLengkap;

    @Column(name = "snapshot_nomor_telepon")
    private String nomorTelepon;

    @Column(name = "snapshot_alamat_lengkap", columnDefinition = "TEXT")
    private String alamatLengkap;

    @Column(name = "snapshot_provinsi")
    private String provinsi;

    @Column(name = "snapshot_kota")
    private String kota;

    @Column(name = "snapshot_kecamatan")
    private String kecamatan;

    @Column(name = "snapshot_kelurahan")
    private String kelurahan;

    @Column(name = "snapshot_kode_pos")
    private String kodePos;
}
