package com.projekfajar.ongkir.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** RAJAONGKIR / TARIF_TETAP / GRATIS_MINIMAL_BELANJA — lihat OngkirCalculationService. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HasilOngkir {
    private BigDecimal tarif;
    private Integer estimasiHari;
    private String sumber;

    // Cuma terisi kalau sumber=RAJAONGKIR — identitas kurir/layanan yang
    // benar-benar dipakai, dibutuhkan pembeli untuk melihat opsi mana yang
    // dipilih (lihat OngkirCalculationService#hitung dengan kurirCode/layanan).
    private String kurirCode;
    private String kurirName;
    private String layanan;
    private String deskripsiLayanan;

    /** Semua opsi RajaOngkir untuk rute ini. Kosong kalau sumber=TARIF_TETAP. */
    @Builder.Default
    private List<PilihanOngkirResponse> opsi = List.of();
}
