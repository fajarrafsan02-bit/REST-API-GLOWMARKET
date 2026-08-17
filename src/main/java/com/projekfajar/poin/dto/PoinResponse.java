package com.projekfajar.poin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.projekfajar.voucher.dto.VoucherResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoinResponse {
    private Long saldoPoin;
    private Long totalDiperoleh;
    private Long totalDipakai;
    private List<RiwayatPoinItem> riwayat;
    private List<VoucherResponse> vouchers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiwayatPoinItem {
        private Long id;
        private Long jumlah;
        private String keterangan;
        private LocalDateTime createdAt;
    }
}
