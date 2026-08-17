package com.projekfajar.tracking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.chat.service.ChatService;
import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.tracking.dto.TrackingResponse;
import com.projekfajar.tracking.model.TrackingPengiriman;
import com.projekfajar.tracking.model.TrackingStatus;
import com.projekfajar.tracking.repository.TrackingPengirimanRepository;
import com.projekfajar.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tracking resi otomatis.
 *
 * Saat pesanan dikirim (DIKIRIM) timeline dibuat dengan tahap awal DIPROSES.
 * Scheduler kemudian memajukan tahapan berdasarkan waktu sejak dikirim,
 * sehingga pengguna melihat perjalanan paket tanpa campur tangan manual.
 * Tahap DITERIMA baru ditulis saat pesanan benar-benar ditutup (SELESAI).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    /** Batas jam sejak pengiriman untuk tiap tahapan perjalanan. */
    private static final long JAM_DALAM_PERJALANAN = 6;
    private static final long JAM_SAMPAI_KOTA_TUJUAN = 18;
    private static final long JAM_OUT_FOR_DELIVERY = 30;

    private final TrackingPengirimanRepository trackingRepository;
    private final PesananRepository pesananRepository;
    private final ChatService chatService;
    private final SettingService settingService;

    /** Membuat timeline awal saat pesanan dikirim (dipanggil dari PesananService). */
    @Transactional
    public void buatTrackingPesanan(Pesanan pesanan, String nomorResi) {
        trackingRepository.deleteByPesananId(pesanan.getId());

        TrackingPengiriman awal = TrackingPengiriman.builder()
                .pesanan(pesanan)
                .nomorResi(nomorResi)
                .status(TrackingStatus.DIPROSES)
                .keterangan("Paket telah diserahkan ke kurir dan sedang diproses di gudang")
                .lokasi("Gudang Pengirim")
                .updatedAt(LocalDateTime.now())
                .build();

        trackingRepository.save(awal);
        log.info("Timeline tracking dibuat untuk pesanan {} (resi {})",
                pesanan.getNomorPesanan(), nomorResi);
    }

    /** Menutup timeline dengan DITERIMA saat pesanan selesai. */
    @Transactional
    public void tutupDiterima(Pesanan pesanan) {
        TrackingPengiriman terakhir = trackingRepository
                .findTopByPesananIdOrderByIdDesc(pesanan.getId())
                .orElse(null);

        if (terakhir != null && terakhir.getStatus() == TrackingStatus.DITERIMA) {
            return;
        }

        String resi = pesanan.getNomorResi();
        if (resi == null || resi.isBlank()) {
            resi = terakhir != null ? terakhir.getNomorResi() : null;
        }

        trackingRepository.save(TrackingPengiriman.builder()
                .pesanan(pesanan)
                .nomorResi(resi)
                .status(TrackingStatus.DITERIMA)
                .keterangan("Paket telah diterima penerima")
                .lokasi(kotaTujuan(pesanan))
                .updatedAt(LocalDateTime.now())
                .build());
        log.info("Tracking pesanan {} ditutup dengan status DITERIMA",
                pesanan.getNomorPesanan());
    }

    @Transactional(readOnly = true)
    public List<TrackingResponse> getByPesanan(Long pesananId) {
        return trackingRepository.findByPesananIdOrderByIdAsc(pesananId).stream()
                .map(TrackingResponse::from)
                .toList();
    }

    /**
     * Memajukan timeline satu tahap — dipakai simulasi/demo oleh admin
     * (misal mengetes alur tanpa menunggu scheduler).
     */
    @Transactional
    public TrackingResponse lanjutkanStatus(Long pesananId) {
        Pesanan pesanan = pesananRepository.findById(pesananId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));

        if (pesanan.getStatus() != OrderStatus.DIKIRIM
                && pesanan.getStatus() != OrderStatus.SELESAI) {
            throw new BusinessException(
                    "Tracking hanya bisa dilanjutkan untuk pesanan yang dikirim atau selesai");
        }

        TrackingPengiriman terakhir = trackingRepository
                .findTopByPesananIdOrderByIdDesc(pesananId)
                .orElseThrow(() -> new BusinessException("Pesanan belum punya timeline tracking"));

        if (terakhir.getStatus() == TrackingStatus.DITERIMA) {
            throw new BusinessException("Paket sudah diterima, tracking tidak bisa dilanjutkan");
        }

        boolean pesananSelesai = pesanan.getStatus() == OrderStatus.SELESAI;
        TrackingStatus berikut = tahapBerikutnya(terakhir.getStatus(), pesananSelesai);

        TrackingPengiriman baru = TrackingPengiriman.builder()
                .pesanan(pesanan)
                .nomorResi(terakhir.getNomorResi())
                .status(berikut)
                .keterangan(keteranganUntuk(berikut, pesanan))
                .lokasi(lokasiUntuk(berikut, pesanan))
                .updatedAt(LocalDateTime.now())
                .build();

        TrackingPengiriman saved = trackingRepository.save(baru);
        log.info("Tracking pesanan {} dilanjutkan ke {}", pesanan.getNomorPesanan(), berikut);

        if (berikut == TrackingStatus.DALAM_PERJALANAN) {
            kirimPemberitahuanDiserahkanKeKurir(pesanan);
        }

        return TrackingResponse.from(saved);
    }

    /**
     * Scheduler memajukan tahapan tracking secara otomatis berdasarkan jam
     * sejak dikirim. Berjalan tiap menit; idempoten karena hanya menambah
     * tahap yang belum tercatat.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void perbaruiTrackingOtomatis() {
        List<Pesanan> sedangDikirim = pesananRepository.findByStatus(OrderStatus.DIKIRIM);
        LocalDateTime now = LocalDateTime.now();
        int diperbarui = 0;

        for (Pesanan pesanan : sedangDikirim) {
            if (pesanan.getDikirimAt() == null) {
                continue;
            }
            try {
                if (majukanTimeline(pesanan, now)) {
                    diperbarui++;
                }
            } catch (Exception e) {
                log.error("Gagal memperbarui tracking pesanan {}: {}",
                        pesanan.getNomorPesanan(), e.getMessage());
            }
        }

        if (diperbarui > 0) {
            log.info("Tracking otomatis: {} pesanan maju satu tahap", diperbarui);
        }
    }

    private boolean majukanTimeline(Pesanan pesanan, LocalDateTime now) {
        long jam = Duration.between(pesanan.getDikirimAt(), now).toHours();

        TrackingStatus target = jam < JAM_DALAM_PERJALANAN
                ? TrackingStatus.DIPROSES
                : jam < JAM_SAMPAI_KOTA_TUJUAN
                        ? TrackingStatus.DALAM_PERJALANAN
                        : jam < JAM_OUT_FOR_DELIVERY
                                ? TrackingStatus.SAMPAI_KOTA_TUJUAN
                                : TrackingStatus.OUT_FOR_DELIVERY;

        TrackingPengiriman terakhir = trackingRepository
                .findTopByPesananIdOrderByIdDesc(pesanan.getId())
                .orElse(null);

        if (terakhir == null || terakhir.getStatus().ordinal() >= target.ordinal()) {
            return false;
        }

        TrackingStatus berikut = tahapBerikutnya(terakhir.getStatus(), false);
        trackingRepository.save(TrackingPengiriman.builder()
                .pesanan(pesanan)
                .nomorResi(pesanan.getNomorResi())
                .status(berikut)
                .keterangan(keteranganUntuk(berikut, pesanan))
                .lokasi(lokasiUntuk(berikut, pesanan))
                .updatedAt(now)
                .build());

        if (berikut == TrackingStatus.DALAM_PERJALANAN) {
            kirimPemberitahuanDiserahkanKeKurir(pesanan);
        }

        return true;
    }

    /**
     * Memberi tahu pelanggan lewat chat (atas nama admin) bahwa paketnya sudah
     * diserahkan ke kurir, tepat saat tracking masuk tahap dalam perjalanan.
     */
    private void kirimPemberitahuanDiserahkanKeKurir(Pesanan pesanan) {
        try {
            User pelanggan = pesanan.getUser();
            if (pelanggan == null) {
                return;
            }

            String namaToko = settingService.getValue("store.name");
            if (namaToko == null || namaToko.isBlank()) {
                namaToko = "toko kami";
            }

            String sapaan = pelanggan.getNamaLengkap() != null && !pelanggan.getNamaLengkap().isBlank()
                    ? "Halo " + pelanggan.getNamaLengkap()
                    : "Halo";

            StringBuilder pesan = new StringBuilder()
                    .append(sapaan).append("! Pesanan ").append(pesanan.getNomorPesanan())
                    .append(" sudah kami serahkan ke kurir dan sedang dalam perjalanan");

            if (pesanan.getNomorResi() != null && !pesanan.getNomorResi().isBlank()) {
                pesan.append(" (resi: ").append(pesanan.getNomorResi()).append(")");
            }

            pesan.append(". Terima kasih sudah berbelanja di ").append(namaToko).append(".");

            chatService.kirimPesanOtomatisDariAdmin(pelanggan, pesan.toString());
        } catch (Exception e) {
            log.error("Gagal mengirim pemberitahuan kurir untuk pesanan {}: {}",
                    pesanan.getId(), e.getMessage());
        }
    }

    private TrackingStatus tahapBerikutnya(TrackingStatus sekarang, boolean pesananSelesai) {
        if (pesananSelesai) {
            return TrackingStatus.DITERIMA;
        }
        return switch (sekarang) {
            case DIPROSES -> TrackingStatus.DALAM_PERJALANAN;
            case DALAM_PERJALANAN -> TrackingStatus.SAMPAI_KOTA_TUJUAN;
            case SAMPAI_KOTA_TUJUAN -> TrackingStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> TrackingStatus.DITERIMA;
            case DITERIMA -> TrackingStatus.DITERIMA;
        };
    }

    private String keteranganUntuk(TrackingStatus status, Pesanan pesanan) {
        return switch (status) {
            case DIPROSES -> "Paket telah diserahkan ke kurir dan sedang diproses di gudang";
            case DALAM_PERJALANAN -> "Paket dalam perjalanan menuju " + kotaTujuan(pesanan);
            case SAMPAI_KOTA_TUJUAN -> "Paket telah tiba di kantor kurir " + kotaTujuan(pesanan);
            case OUT_FOR_DELIVERY -> "Paket sedang diantar oleh kurir ke alamat penerima";
            case DITERIMA -> "Paket telah diterima penerima";
        };
    }

    private String lokasiUntuk(TrackingStatus status, Pesanan pesanan) {
        return switch (status) {
            case DIPROSES -> "Gudang Pengirim";
            case DALAM_PERJALANAN -> "Dalam Perjalanan";
            case SAMPAI_KOTA_TUJUAN -> "Kantor Kurir " + kotaTujuan(pesanan);
            case OUT_FOR_DELIVERY -> kotaTujuan(pesanan);
            case DITERIMA -> kotaTujuan(pesanan);
        };
    }

    private String kotaTujuan(Pesanan pesanan) {
        if (pesanan.getAlamatSnapshot() != null && pesanan.getAlamatSnapshot().getKota() != null
                && !pesanan.getAlamatSnapshot().getKota().isBlank()) {
            return pesanan.getAlamatSnapshot().getKota();
        }
        return "Kota Tujuan";
    }
}
