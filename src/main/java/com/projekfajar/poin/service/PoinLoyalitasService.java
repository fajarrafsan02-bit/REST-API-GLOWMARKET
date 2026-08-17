package com.projekfajar.poin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.poin.dto.PoinResponse;
import com.projekfajar.poin.model.PoinUser;
import com.projekfajar.poin.model.RiwayatPoin;
import com.projekfajar.poin.repository.PoinUserRepository;
import com.projekfajar.poin.repository.RiwayatPoinRepository;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.voucher.model.Voucher;
import com.projekfajar.voucher.repository.VoucherRepository;
import com.projekfajar.voucher.service.VoucherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Poin loyalitas: user mendapat poin dari pembelian yang lunas dan bisa
 * menukarnya menjadi voucher diskon nominal yang terikat pada dirinya.
 *
 * Konversi: 1 poin per Rp 10.000 pembelian; tiap 100 poin ditukar menjadi
 * voucher senilai Rp 10.000. Semua hitungan dilakukan server-side.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PoinLoyalitasService {

    /** Rupiah belanja yang menghasilkan 1 poin. */
    public static final BigDecimal BELANJA_PER_POIN = BigDecimal.valueOf(10_000);

    /** Poin minimum dan kelipatan penukaran. */
    public static final long POIN_MIN_TUKAR = 100;

    /** Nilai rupiah per poin saat ditukar menjadi voucher. */
    public static final long RUPIAH_PER_POIN = 100;

    /** Masa berlaku voucher hasil tukar, dalam hari. */
    public static final long VOUCHER_BERLAKU_HARI = 30;

    private final PoinUserRepository poinUserRepository;
    private final RiwayatPoinRepository riwayatPoinRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherService voucherService;

    /**
     * Mencatat poin dari sebuah pesanan yang sudah lunas. Dipanggil dari
     * PesananService tepat setelah penjualan dipertanggungjawabkan.
     */
    @Transactional
    public void tambahPoinDariPembelian(Pesanan pesanan) {
        log.debug("Awarding loyalty points for order nomor={} total={}",
                pesanan.getNomorPesanan(), pesanan.getTotalHarga());
        User user = pesanan.getUser();
        if (user == null || pesanan.getTotalHarga() == null) {
            log.warn("Cannot award points for order nomor={}: missing user or total",
                    pesanan.getNomorPesanan());
            return;
        }

        long poin = pesanan.getTotalHarga()
                .divide(BELANJA_PER_POIN, 0, RoundingMode.FLOOR)
                .longValue();
        if (poin <= 0) {
            log.debug("No points earned for order nomor={} (total={})",
                    pesanan.getNomorPesanan(), pesanan.getTotalHarga());
            return;
        }

        PoinUser saldo = dapatkanAtauBuat(user);
        saldo.setSaldoPoin(saldo.getSaldoPoin() + poin);
        saldo.setTotalDiperoleh(saldo.getTotalDiperoleh() + poin);
        saldo.setUpdatedAt(LocalDateTime.now());
        poinUserRepository.save(saldo);

        riwayatPoinRepository.save(RiwayatPoin.builder()
                .user(user)
                .pesanan(pesanan)
                .jumlah(poin)
                .keterangan("Poin dari pembelian " + pesanan.getNomorPesanan())
                .build());

        log.info("Awarded {} points to userId={} from order nomor={}, new balance={}",
                poin, user.getId(), pesanan.getNomorPesanan(), saldo.getSaldoPoin());
    }

    /** Saldo, riwayat, dan voucher milik user. */
    @Transactional(readOnly = true)
    public PoinResponse getStatus(Long userId) {
        log.debug("Fetching loyalty status for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        PoinUser saldo = poinUserRepository.findByUserId(userId)
                .orElseGet(() -> PoinUser.builder().user(user).build());

        return PoinResponse.builder()
                .saldoPoin(saldo.getSaldoPoin())
                .totalDiperoleh(saldo.getTotalDiperoleh())
                .totalDipakai(saldo.getTotalDipakai())
                .riwayat(riwayatPoinRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(r -> PoinResponse.RiwayatPoinItem.builder()
                                .id(r.getId())
                                .jumlah(r.getJumlah())
                                .keterangan(r.getKeterangan())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList())
                .vouchers(voucherService.getByUser(userId))
                .build();
    }

    /** Menukar poin menjadi voucher diskon nominal yang terikat pada user. */
    @Transactional
    public Voucher tukarVoucher(Long userId, long jumlahPoin) {
        log.info("Redeeming {} points to voucher for userId={}", jumlahPoin, userId);
        if (jumlahPoin % POIN_MIN_TUKAR != 0) {
            log.warn("Invalid redeem amount {} for userId={}, must be multiple of {}",
                    jumlahPoin, userId, POIN_MIN_TUKAR);
            throw new BusinessException(
                    "Jumlah poin harus kelipatan " + POIN_MIN_TUKAR);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));

        PoinUser saldo = dapatkanAtauBuat(user);
        if (saldo.getSaldoPoin() < jumlahPoin) {
            log.warn("Insufficient points for userId={}: requested={} balance={}",
                    userId, jumlahPoin, saldo.getSaldoPoin());
            throw new BusinessException("Saldo poin tidak cukup");
        }

        long nilaiRupiah = jumlahPoin * RUPIAH_PER_POIN;
        Voucher voucher = Voucher.builder()
                .kode(buatKodeVoucher())
                .jenis(Voucher.JENIS_NOMINAL)
                .nilai(BigDecimal.valueOf(nilaiRupiah))
                .minBelanja(BigDecimal.ZERO)
                .kuota(1)
                .terpakai(0)
                .aktif(true)
                .berlakuDari(LocalDateTime.now())
                .berlakuSampai(LocalDateTime.now().plusDays(VOUCHER_BERLAKU_HARI))
                .user(user)
                .build();
        Voucher saved = voucherRepository.save(voucher);

        saldo.setSaldoPoin(saldo.getSaldoPoin() - jumlahPoin);
        saldo.setTotalDipakai(saldo.getTotalDipakai() + jumlahPoin);
        saldo.setUpdatedAt(LocalDateTime.now());
        poinUserRepository.save(saldo);

        riwayatPoinRepository.save(RiwayatPoin.builder()
                .user(user)
                .voucher(saved)
                .jumlah(-jumlahPoin)
                .keterangan("Tukar " + jumlahPoin + " poin menjadi voucher " + saved.getKode())
                .build());

        log.info("Points redeemed: userId={} points={} voucherCode={} value={} remainingBalance={}",
                userId, jumlahPoin, saved.getKode(), nilaiRupiah, saldo.getSaldoPoin());
        return saved;
    }

    private PoinUser dapatkanAtauBuat(User user) {
        return poinUserRepository.findByUserId(user.getId())
                .orElseGet(() -> poinUserRepository.save(
                        PoinUser.builder().user(user).build()));
    }

    private String buatKodeVoucher() {
        String kode;
        do {
            kode = "LOY-" + ThreadLocalRandom.current()
                    .ints('A', 'Z' + 1)
                    .limit(6)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString()
                    .toUpperCase(Locale.ROOT);
        } while (voucherRepository.findByKode(kode).isPresent());
        return kode;
    }
}
