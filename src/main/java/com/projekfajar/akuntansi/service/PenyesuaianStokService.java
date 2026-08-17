package com.projekfajar.akuntansi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.akuntansi.model.SumberJurnal;
import com.projekfajar.akuntansi.repository.JurnalRepository;
import com.projekfajar.akuntansi.service.JurnalService.Baris;
import com.projekfajar.produk.model.Produk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Menjurnal perubahan stok yang terjadi di luar pembelian dan penjualan:
 * admin memperbaiki angka stok, barang rusak, atau produk baru dibuat dengan
 * stok awal.
 *
 * Sebelum jurnal pembuka ada, stok fisik sengaja tidak dijurnal di sini:
 * nilainya akan masuk lewat saldo awal. Kalau keduanya menulis, persediaan
 * tercatat dua kali.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PenyesuaianStokService {

    public static final String AKUN_PERSEDIAAN = "1-200";
    public static final String AKUN_MODAL = "3-100";
    public static final String AKUN_SELISIH = "6-500";

    private final JurnalService jurnalService;
    private final JurnalRepository jurnalRepository;

    @Transactional
    public void catat(Produk produk, Integer stokLama, Integer stokBaru, String alasan) {
        int lama = stokLama != null ? stokLama : 0;
        int baru = stokBaru != null ? stokBaru : 0;
        int selisih = baru - lama;

        if (selisih == 0) {
            return;
        }

        if (!jurnalRepository.existsBySumber(SumberJurnal.SALDO_AWAL)) {
            log.info("Penyesuaian stok {} ditunda: saldo awal belum dicatat",
                    produk.getNama());
            return;
        }

        BigDecimal modal = produk.getHargaModal() != null
                ? produk.getHargaModal()
                : BigDecimal.ZERO;

        if (modal.signum() <= 0) {
            // Barang tanpa harga modal tidak punya nilai yang bisa dibukukan.
            // Lebih baik tidak menjurnal daripada mencatat persediaan senilai nol.
            log.warn("Penyesuaian stok {} tidak dijurnal: harga modalnya belum diisi",
                    produk.getNama());
            return;
        }

        BigDecimal nilai = modal.multiply(BigDecimal.valueOf(Math.abs(selisih)));
        String keterangan = "Penyesuaian stok " + produk.getNama()
                + " (" + lama + " → " + baru + ")"
                + (alasan != null && !alasan.isBlank() ? " — " + alasan : "");

        List<Baris> baris = selisih > 0
                // Barang bertambah tanpa pembelian dianggap setoran pemilik.
                // Dikreditkan ke modal, bukan ke pendapatan, supaya penambahan
                // stok tidak pernah tampak sebagai keuntungan.
                ? List.of(
                        Baris.debit(AKUN_PERSEDIAAN, nilai, keterangan),
                        Baris.kredit(AKUN_MODAL, nilai, keterangan))
                // Barang berkurang tanpa penjualan adalah kerugian nyata.
                : List.of(
                        Baris.debit(AKUN_SELISIH, nilai, keterangan),
                        Baris.kredit(AKUN_PERSEDIAAN, nilai, keterangan));

        jurnalService.catat(LocalDate.now(), keterangan, SumberJurnal.PENYESUAIAN,
                produk.getId(), baris);

        log.info("Penyesuaian stok {} dijurnal: {} unit senilai {}",
                produk.getNama(), selisih, nilai);
    }
}
