package com.projekfajar.payment.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Nama customer tidak boleh kosong")
    private String customerName;

    @NotBlank(message = "Email customer tidak boleh kosong")
    private String customerEmail;

    private String customerPhone;

    @NotNull(message = "Total amount tidak boleh kosong")
    // @Min(value = 10000, message = "Minimum pembayaran Rp 10.000")
    private BigDecimal amount;

    private BigDecimal ongkir;

    private String description;

    // Optional: untuk payment dari keranjang
    private Long userId;

    // Shipping details
    private Long alamatId;
    private String catatan;

    // Pilihan kurir RajaOngkir dari pembeli (opsional) — kosong berarti
    // otomatis termurah, perilaku yang sama seperti sebelum fitur pilih-kurir.
    // Harga TIDAK dipercaya dari sini; server selalu mengambil ulang harga
    // sungguhan dari RajaOngkir untuk kombinasi ini (lihat OngkirCalculationService).
    private String kurirCode;
    private String layanan;

    // Kode voucher diskon (opsional). Potongan dihitung ulang di server,
    // nilai dari client tidak dipercaya (lihat VoucherService).
    private String kodeVoucher;

    // Metode pembayaran tunggal yang dipilih pembeli di Checkout (opsional,
    // mis. "BCA", "QRIS"). Kalau diisi, halaman Xendit langsung dipersempit
    // ke metode itu saja alih-alih menampilkan semua metode aktif toko.
    private String paymentMethod;
}
