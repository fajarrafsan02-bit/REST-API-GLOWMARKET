package com.projekfajar.tracking.dto;

import java.time.LocalDateTime;

import com.projekfajar.tracking.model.TrackingPengiriman;
import com.projekfajar.tracking.model.TrackingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {
    private Long id;
    private Long pesananId;
    private String nomorResi;
    private TrackingStatus status;
    private String keterangan;
    private String lokasi;
    private LocalDateTime updatedAt;

    public static TrackingResponse from(TrackingPengiriman t) {
        return TrackingResponse.builder()
                .id(t.getId())
                .pesananId(t.getPesanan().getId())
                .nomorResi(t.getNomorResi())
                .status(t.getStatus())
                .keterangan(t.getKeterangan())
                .lokasi(t.getLokasi())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
