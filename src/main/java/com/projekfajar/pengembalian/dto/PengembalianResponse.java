package com.projekfajar.pengembalian.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.projekfajar.pengembalian.model.Pengembalian;
import com.projekfajar.pengembalian.model.PengembalianStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PengembalianResponse {
    private Long id;
    private String nomorPengembalian;
    private Long pesananId;
    private String nomorPesanan;
    private Long userId;
    private String alasan;
    private PengembalianStatus status;
    private BigDecimal jumlahRefund;
    private String catatanAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime diterimaAt;

    public static PengembalianResponse from(Pengembalian p) {
        return PengembalianResponse.builder()
                .id(p.getId())
                .nomorPengembalian(p.getNomorPengembalian())
                .pesananId(p.getPesanan().getId())
                .nomorPesanan(p.getPesanan().getNomorPesanan())
                .userId(p.getUser().getId())
                .alasan(p.getAlasan())
                .status(p.getStatus())
                .jumlahRefund(p.getJumlahRefund())
                .catatanAdmin(p.getCatatanAdmin())
                .createdAt(p.getCreatedAt())
                .approvedAt(p.getApprovedAt())
                .diterimaAt(p.getDiterimaAt())
                .build();
    }
}
