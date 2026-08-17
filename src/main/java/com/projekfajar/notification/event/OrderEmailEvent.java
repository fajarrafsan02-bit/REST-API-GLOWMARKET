package com.projekfajar.notification.event;

import com.projekfajar.pesanan.model.OrderStatus;

/**
 * Sinyal bahwa sebuah pesanan perlu diberitahukan lewat email.
 *
 * Hanya membawa id — bukan entity — karena email dikirim setelah transaksi
 * commit di thread lain, sehingga entity dari sesi lama tidak lagi bisa dibaca.
 * Penerimanya memuat ulang pesanan lengkap dengan itemnya.
 *
 * @param pesananId  pesanan yang bersangkutan
 * @param jenis      pembayaran lunas atau perubahan status
 * @param status     status baru (diisi untuk jenis PERUBAHAN_STATUS)
 * @param dariPending true bila status berpindah dari PENDING, dipakai untuk
 *                    mencegah email "sedang disiapkan" menyusul email
 *                    "pembayaran berhasil" yang terjadi hampir bersamaan
 */
public record OrderEmailEvent(
        Long pesananId,
        Jenis jenis,
        OrderStatus status,
        boolean dariPending) {

    public enum Jenis {
        PEMBAYARAN_LUNAS,
        PERUBAHAN_STATUS
    }

    public static OrderEmailEvent pembayaranLunas(Long pesananId) {
        return new OrderEmailEvent(pesananId, Jenis.PEMBAYARAN_LUNAS, null, false);
    }

    public static OrderEmailEvent perubahanStatus(Long pesananId, OrderStatus status, boolean dariPending) {
        return new OrderEmailEvent(pesananId, Jenis.PERUBAHAN_STATUS, status, dariPending);
    }
}
