package com.projekfajar.ongkir.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Satu opsi kurir+layanan yang bisa dipilih pembeli, dipetakan langsung dari
 * RajaOngkirCostResponse.Tarif. Dikembalikan sebagai daftar oleh
 * OngkirCalculationService#hitungSemuaOpsi supaya Keranjang/Checkout bisa
 * menampilkan pilihan alih-alih cuma yang termurah.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PilihanOngkirResponse {
    private String kurirCode;
    private String kurirName;
    private String layanan;
    private String deskripsi;
    private BigDecimal tarif;
    private Integer estimasiHari;
}
