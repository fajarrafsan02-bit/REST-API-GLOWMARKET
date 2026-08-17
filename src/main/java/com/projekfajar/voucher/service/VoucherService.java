package com.projekfajar.voucher.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.exception.BusinessException;
import com.projekfajar.exception.ResourceNotFoundException;
import com.projekfajar.voucher.dto.VoucherRequest;
import com.projekfajar.voucher.dto.VoucherResponse;
import com.projekfajar.voucher.model.Voucher;
import com.projekfajar.voucher.repository.VoucherRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Voucher diskon: validasi kode, hitung potongan, dan pencatatan pemakaian.
 *
 * Hitungan diskon dilakukan di sini (bukan dari client) — checkout hanya
 * mengirim kode, server yang menghitung potongan terhadap subtotal yang
 * sudah diverifikasi, sehingga nominal tidak bisa dipalsukan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;

    /** Hasil validasi + hitungan diskon untuk sebuah subtotal. */
    public record HasilDiskon(Voucher voucher, BigDecimal diskon, String pesan) {
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getAll() {
        return voucherRepository.findAll().stream()
                .map(VoucherResponse::from)
                .toList();
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        if (voucherRepository.findByKode(request.getKode()).isPresent()) {
            throw new BusinessException("Kode voucher sudah terdaftar");
        }

        Voucher voucher = Voucher.builder()
                .kode(normalisasiKode(request.getKode()))
                .jenis(normalisasiJenis(request.getJenis()))
                .nilai(request.getNilai())
                .minBelanja(request.getMinBelanja())
                .maksDiskon(request.getMaksDiskon())
                .kuota(request.getKuota())
                .aktif(request.getAktif() != null ? request.getAktif() : true)
                .berlakuDari(request.getBerlakuDari())
                .berlakuSampai(request.getBerlakuSampai())
                .createdAt(LocalDateTime.now())
                .build();

        Voucher saved = voucherRepository.save(voucher);
        log.info("Voucher {} dibuat ({} {})", saved.getKode(), saved.getJenis(), saved.getNilai());
        return VoucherResponse.from(saved);
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher tidak ditemukan"));

        voucherRepository.findByKode(request.getKode())
                .filter(v -> !v.getId().equals(id))
                .ifPresent(v -> {
                    throw new BusinessException("Kode voucher sudah terdaftar");
                });

        voucher.setKode(normalisasiKode(request.getKode()));
        voucher.setJenis(normalisasiJenis(request.getJenis()));
        voucher.setNilai(request.getNilai());
        voucher.setMinBelanja(request.getMinBelanja());
        voucher.setMaksDiskon(request.getMaksDiskon());
        voucher.setKuota(request.getKuota());
        voucher.setAktif(request.getAktif() != null ? request.getAktif() : voucher.getAktif());
        voucher.setBerlakuDari(request.getBerlakuDari());
        voucher.setBerlakuSampai(request.getBerlakuSampai());
        voucher.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(voucher);
        log.info("Voucher {} diperbarui", saved.getKode());
        return VoucherResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher tidak ditemukan"));
        voucherRepository.delete(voucher);
        log.info("Voucher {} dihapus", voucher.getKode());
    }

    @Transactional
    public VoucherResponse toggleAktif(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher tidak ditemukan"));
        voucher.setAktif(!Boolean.TRUE.equals(voucher.getAktif()));
        voucher.setUpdatedAt(LocalDateTime.now());
        Voucher saved = voucherRepository.save(voucher);
        log.info("Voucher {} aktif = {}", saved.getKode(), saved.getAktif());
        return VoucherResponse.from(saved);
    }

    /**
     * Memvalidasi kode voucher dan menghitung potongan untuk subtotal tertentu.
     * Tidak mengubah data apa pun — murni untuk cek di halaman checkout.
     * Tanpa konteks user, voucher ber-pemilik ditolak.
     */
    @Transactional(readOnly = true)
    public HasilDiskon hitung(String kode, BigDecimal subtotal) {
        return hitung(kode, subtotal, null);
    }

    /**
     * Sama seperti {@link #hitung(String, BigDecimal)} tetapi memvalidasi
     * kepemilikan: voucher hasil tukar poin hanya bisa dipakai oleh pemiliknya.
     */
    @Transactional(readOnly = true)
    public HasilDiskon hitung(String kode, BigDecimal subtotal, Long userId) {
        Voucher voucher = cariVoucherAktif(kode);
        validasiPemilik(voucher, userId);
        validasiMinBelanja(voucher, subtotal);

        BigDecimal diskon = hitungDiskon(voucher, subtotal);
        return new HasilDiskon(voucher, diskon, "Voucher berhasil dipakai");
    }

    /** Voucher yang terikat pada user (hasil tukar poin), terbaru di atas. */
    @Transactional(readOnly = true)
    public List<VoucherResponse> getByUser(Long userId) {
        return voucherRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(VoucherResponse::from)
                .toList();
    }

    /** Voucher umum yang aktif dan bisa dipakai siapa saja. */
    @Transactional(readOnly = true)
    public List<VoucherResponse> getPublicAktif() {
        return voucherRepository.findPublicAktif(LocalDateTime.now()).stream()
                .map(VoucherResponse::from)
                .toList();
    }

    private void validasiPemilik(Voucher voucher, Long userId) {
        if (voucher.getUser() == null) {
            return;
        }
        if (userId == null || !voucher.getUser().getId().equals(userId)) {
            throw new BusinessException("Voucher ini hanya bisa dipakai oleh pemiliknya");
        }
    }

    /**
     * Mencatat pemakaian voucher pada checkout yang berhasil dibuat invoice.
     * Dipanggil setelah pesanan PENDING tersimpan (bukan saat kode dicek),
     * supaya kuota tidak hangus hanya karena orang mengetik kode.
     */
    @Transactional
    public void pakai(String kode) {
        if (kode == null || kode.isBlank()) {
            return;
        }
        Voucher voucher = voucherRepository.findByKode(kode.trim().toUpperCase(Locale.ROOT))
                .orElse(null);
        if (voucher == null) {
            log.warn("Voucher {} dipakai tetapi tidak ditemukan", kode);
            return;
        }

        int terpakai = voucher.getTerpakai() != null ? voucher.getTerpakai() : 0;
        voucher.setTerpakai(terpakai + 1);
        voucher.setUpdatedAt(LocalDateTime.now());
        voucherRepository.save(voucher);
        log.info("Voucher {} terpakai (total {})", voucher.getKode(), voucher.getTerpakai());
    }

    private Voucher cariVoucherAktif(String kode) {
        if (kode == null || kode.isBlank()) {
            throw new BusinessException("Masukkan kode voucher");
        }

        Voucher voucher = voucherRepository.findByKode(kode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException("Kode voucher tidak ditemukan"));

        if (!Boolean.TRUE.equals(voucher.getAktif())) {
            throw new BusinessException("Voucher sudah tidak aktif");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getBerlakuDari() != null && now.isBefore(voucher.getBerlakuDari())) {
            throw new BusinessException("Voucher belum berlaku");
        }
        if (voucher.getBerlakuSampai() != null && now.isAfter(voucher.getBerlakuSampai())) {
            throw new BusinessException("Voucher sudah kedaluwarsa");
        }

        if (voucher.getKuota() != null
                && (voucher.getTerpakai() != null && voucher.getTerpakai() >= voucher.getKuota())) {
            throw new BusinessException("Kuota voucher sudah habis");
        }

        return voucher;
    }

    private void validasiMinBelanja(Voucher voucher, BigDecimal subtotal) {
        if (voucher.getMinBelanja() != null
                && (subtotal == null || subtotal.compareTo(voucher.getMinBelanja()) < 0)) {
            throw new BusinessException(
                    "Voucher hanya berlaku untuk belanja minimal Rp "
                            + voucher.getMinBelanja().toBigInteger().toString());
        }
    }

    /** Menghitung potongan: PERSEN (dibatasi maksDiskon) atau NOMINAL. */
    public BigDecimal hitungDiskon(Voucher voucher, BigDecimal subtotal) {
        BigDecimal dasar = subtotal != null ? subtotal : BigDecimal.ZERO;

        if (Voucher.JENIS_PERSEN.equals(voucher.getJenis())) {
            BigDecimal diskon = dasar.multiply(voucher.getNilai())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaksDiskon() != null
                    && diskon.compareTo(voucher.getMaksDiskon()) > 0) {
                diskon = voucher.getMaksDiskon();
            }
            return diskon;
        }

        return voucher.getNilai().min(dasar);
    }

    private String normalisasiKode(String kode) {
        return kode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalisasiJenis(String jenis) {
        if (Voucher.JENIS_PERSEN.equalsIgnoreCase(jenis)) {
            return Voucher.JENIS_PERSEN;
        }
        if (Voucher.JENIS_NOMINAL.equalsIgnoreCase(jenis)) {
            return Voucher.JENIS_NOMINAL;
        }
        throw new BusinessException("Jenis voucher harus PERSEN atau NOMINAL");
    }
}
