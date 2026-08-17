package com.projekfajar.akuntansi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BebanResponse {
    private Long id;
    private LocalDate tanggal;
    private String kodeAkun;
    private String namaAkun;
    private String keterangan;
    private BigDecimal jumlah;
    private boolean dibatalkan;
    private LocalDateTime createdAt;
}
