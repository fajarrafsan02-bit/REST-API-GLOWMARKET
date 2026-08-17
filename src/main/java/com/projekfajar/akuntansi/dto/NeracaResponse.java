package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.projekfajar.akuntansi.dto.LabaRugiResponse.BarisAkun;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Neraca per satu tanggal.
 *
 * Tanpa jurnal penutup, laba tahun berjalan dihitung langsung dari selisih
 * pendapatan dan beban sejak awal pembukuan sampai tanggal laporan, lalu
 * dimasukkan ke sisi ekuitas — dengan begitu persamaan neraca tetap seimbang
 * tanpa perlu proses tutup buku yang mudah salah.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeracaResponse {

    private LocalDate sampai;

    private List<BarisAkun> aset;
    private BigDecimal totalAset;

    private List<BarisAkun> liabilitas;
    private BigDecimal totalLiabilitas;

    private List<BarisAkun> ekuitas;
    private BigDecimal labaTahunBerjalan;
    private BigDecimal totalEkuitas;

    private BigDecimal totalLiabilitasDanEkuitas;

    /** Aset − (liabilitas + ekuitas). Harus nol; bila tidak, ada yang salah. */
    private BigDecimal selisih;
    private boolean seimbang;
}
