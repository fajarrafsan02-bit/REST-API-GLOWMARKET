package com.projekfajar.pesanan.model;

public enum OrderStatus {
    PENDING,      // Menunggu pembayaran
    DIKEMAS,      // Sedang dikemas
    DIKIRIM,      // Sedang dikirim
    SELESAI,      // Pesanan selesai/diterima
    DIBATALKAN,   // Pesanan dibatalkan
    DIKEMBALIKAN  // Pesanan dikembalikan
}
