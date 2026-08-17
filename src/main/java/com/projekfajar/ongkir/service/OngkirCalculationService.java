package com.projekfajar.ongkir.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.alamat.model.Alamat;
import com.projekfajar.alamat.service.AlamatOngkirResolver;
import com.projekfajar.ongkir.dto.HasilOngkir;
import com.projekfajar.ongkir.dto.PilihanOngkirResponse;
import com.projekfajar.ongkir.dto.RajaOngkirCostResponse;
import com.projekfajar.ongkir.model.Ongkir;
import com.projekfajar.ongkir.repository.OngkirRepository;
import com.projekfajar.settings.service.SettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Satu titik perhitungan ongkir dipakai baik checkout (XenditService) maupun
 * estimasi Keranjang, supaya keduanya tidak bisa diam-diam berbeda logika.
 * RajaOngkir gagal dengan cara apa pun — tidak dikonfigurasi, alamat gagal
 * dicocokkan, kuota habis, timeout — selalu jatuh ke tarif tetap secara diam-
 * diam; pembeli tidak pernah melihat error karena masalah pihak ketiga.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OngkirCalculationService {

    public static final String SUMBER_RAJAONGKIR = "RAJAONGKIR";
    public static final String SUMBER_TARIF_TETAP = "TARIF_TETAP";
    public static final String SUMBER_GRATIS_MINIMAL_BELANJA = "GRATIS_MINIMAL_BELANJA";

    private final AlamatOngkirResolver alamatOngkirResolver;
    private final RajaOngkirClient rajaOngkirClient;
    private final OngkirRepository ongkirRepository;
    private final SettingService settingService;

    @Transactional
    public HasilOngkir hitung(Alamat alamat, int totalBeratGram, BigDecimal subtotal) {
        HasilOngkir hasil = hitungViaRajaOngkir(alamat, totalBeratGram)
                .orElseGet(() -> hitungViaTarifTetap(alamat));

        return terapkanGratisMinimalBelanja(hasil, subtotal);
    }

    /**
     * Overload pilihan pembeli. Harga dari client tidak dipercaya — tarif selalu
     * diambil ulang dari RajaOngkir untuk kombinasi kurir+layanan itu. Kalau
     * pilihan tidak ketemu (quote basi, dipalsukan, RajaOngkir mati) jatuh ke
     * {@link #hitung(Alamat, int, BigDecimal)} tanpa melempar error.
     */
    @Transactional
    public HasilOngkir hitung(Alamat alamat, int totalBeratGram, BigDecimal subtotal,
            String kurirCode, String layanan) {
        if (kosong(kurirCode) || kosong(layanan)) {
            return hitung(alamat, totalBeratGram, subtotal);
        }

        List<PilihanOngkirResponse> opsi = hitungSemuaOpsi(alamat, totalBeratGram);
        if (opsi.isEmpty() && akanGratis(subtotal)) {
            opsi = opsiDariPengaturanKurir();
        }

        Optional<PilihanOngkirResponse> dipilih = opsi.stream()
                .filter(o -> sama(kurirCode, o.getKurirCode()) && sama(layanan, o.getLayanan()))
                .findFirst();

        if (dipilih.isEmpty()) {
            return hitung(alamat, totalBeratGram, subtotal);
        }

        PilihanOngkirResponse p = dipilih.get();
        HasilOngkir hasil = HasilOngkir.builder()
                .tarif(p.getTarif())
                .estimasiHari(p.getEstimasiHari())
                .sumber(SUMBER_RAJAONGKIR)
                .kurirCode(p.getKurirCode())
                .kurirName(p.getKurirName())
                .layanan(p.getLayanan())
                .deskripsiLayanan(p.getDeskripsi())
                .opsi(opsi)
                .build();

        return terapkanGratisMinimalBelanja(hasil, subtotal);
    }

    /**
     * Semua kombinasi kurir+layanan dari RajaOngkir untuk rute/berat ini.
     * Kosong kalau RajaOngkir tidak bisa dipakai — sama seperti hitungViaRajaOngkir.
     */
    public List<PilihanOngkirResponse> hitungSemuaOpsi(Alamat alamat, int totalBeratGram) {
        return ambilTarifRajaOngkir(alamat, totalBeratGram)
                .map(this::keDaftarPilihan)
                .orElse(List.of());
    }

    private Optional<HasilOngkir> hitungViaRajaOngkir(Alamat alamat, int totalBeratGram) {
        return ambilTarifRajaOngkir(alamat, totalBeratGram).flatMap(list -> {
            Optional<HasilOngkir> hasil = ambilTermurah(list);
            hasil.ifPresent(h -> h.setOpsi(keDaftarPilihan(list)));
            return hasil;
        });
    }

    private Optional<List<RajaOngkirCostResponse.Tarif>> ambilTarifRajaOngkir(Alamat alamat, int totalBeratGram) {
        if (alamat == null) {
            return Optional.empty();
        }

        String originId = settingService.getValue("shipping.origin-rajaongkir-id");
        if (originId == null || originId.isBlank()) {
            return Optional.empty();
        }

        Optional<String> destinationId = alamatOngkirResolver.resolveDestinationId(alamat);
        if (destinationId.isEmpty()) {
            return Optional.empty();
        }

        List<String> kurir = settingService.getList("shipping.rajaongkir-kurir");
        if (kurir == null || kurir.isEmpty()) {
            // Field pengaturan kosong = jangan tebak-nebak di UI admin, tapi
            // tetap cek kurir umum supaya picker JNE/TIKI bisa muncul setelah
            // API key + lokasi asal diisi. Admin bisa membatasi lewat field itu.
            kurir = List.of("jne", "tiki", "pos", "jnt", "sicepat");
        }

        return rajaOngkirClient.hitungOngkir(originId, destinationId.get(), totalBeratGram, kurir);
    }

    private Optional<HasilOngkir> ambilTermurah(List<RajaOngkirCostResponse.Tarif> tarifList) {
        return tarifList.stream()
                .filter(t -> t.getCost() != null)
                .min(Comparator.comparing(RajaOngkirCostResponse.Tarif::getCost))
                .map(this::keHasilRajaOngkir);
    }

    private List<PilihanOngkirResponse> keDaftarPilihan(List<RajaOngkirCostResponse.Tarif> tarifList) {
        return tarifList.stream()
                .filter(t -> t.getCost() != null)
                .map(this::kePilihan)
                .sorted(Comparator.comparing(PilihanOngkirResponse::getTarif)
                        .thenComparing(p -> p.getKurirName() == null ? "" : p.getKurirName())
                        .thenComparing(p -> p.getLayanan() == null ? "" : p.getLayanan()))
                .toList();
    }

    private HasilOngkir keHasilRajaOngkir(RajaOngkirCostResponse.Tarif tarif) {
        return HasilOngkir.builder()
                .tarif(tarif.getCost())
                .estimasiHari(parseEtdHari(tarif.getEtd()))
                .sumber(SUMBER_RAJAONGKIR)
                .kurirCode(tarif.getCode())
                .kurirName(tarif.getName())
                .layanan(tarif.getService())
                .deskripsiLayanan(tarif.getDescription())
                .opsi(List.of())
                .build();
    }

    private PilihanOngkirResponse kePilihan(RajaOngkirCostResponse.Tarif tarif) {
        return PilihanOngkirResponse.builder()
                .kurirCode(tarif.getCode())
                .kurirName(tarif.getName())
                .layanan(tarif.getService())
                .deskripsi(tarif.getDescription())
                .tarif(tarif.getCost())
                .estimasiHari(parseEtdHari(tarif.getEtd()))
                .build();
    }

    private HasilOngkir hitungViaTarifTetap(Alamat alamat) {
        BigDecimal tarif = BigDecimal.ZERO;
        Integer estimasiHari = null;

        if (alamat != null && alamat.getProvinsi() != null) {
            Ongkir ongkir = ongkirRepository.findByProvinsi(alamat.getProvinsi().trim()).orElse(null);
            if (ongkir != null) {
                tarif = ongkir.getTarif();
                estimasiHari = ongkir.getEstimasiHari();
            }
        }

        return HasilOngkir.builder()
                .tarif(tarif)
                .estimasiHari(estimasiHari)
                .sumber(SUMBER_TARIF_TETAP)
                .opsi(List.of())
                .build();
    }

    private HasilOngkir terapkanGratisMinimalBelanja(HasilOngkir hasil, BigDecimal subtotal) {
        int threshold = settingService.getInt("shipping.gratis-ongkir-minimal", 0);
        if (threshold <= 0 || subtotal == null) {
            return hasil;
        }

        if (subtotal.compareTo(BigDecimal.valueOf(threshold)) >= 0) {
            List<PilihanOngkirResponse> opsi = opsiDenganTarifGratis(hasil.getOpsi());
            PilihanOngkirResponse pertama = opsi.isEmpty() ? null : opsi.get(0);
            return HasilOngkir.builder()
                    .tarif(BigDecimal.ZERO)
                    .estimasiHari(hasil.getEstimasiHari())
                    .sumber(SUMBER_GRATIS_MINIMAL_BELANJA)
                    .kurirCode(hasil.getKurirCode() != null ? hasil.getKurirCode()
                            : (pertama != null ? pertama.getKurirCode() : null))
                    .kurirName(hasil.getKurirName() != null ? hasil.getKurirName()
                            : (pertama != null ? pertama.getKurirName() : null))
                    .layanan(hasil.getLayanan() != null ? hasil.getLayanan()
                            : (pertama != null ? pertama.getLayanan() : null))
                    .deskripsiLayanan(hasil.getDeskripsiLayanan())
                    .opsi(opsi)
                    .build();
        }

        return hasil;
    }

    /** "6-7 day" / "2 - 3 days" dsb -> ambil angka terbesar sebagai estimasi konservatif. */
    private Integer parseEtdHari(String etd) {
        if (etd == null || etd.isBlank()) {
            return null;
        }
        try {
            String[] bagianAngka = etd.replaceAll("[^0-9-]", " ").trim().split("[\\s-]+");
            int maksimal = 0;
            for (String bagian : bagianAngka) {
                if (!bagian.isBlank()) {
                    maksimal = Math.max(maksimal, Integer.parseInt(bagian));
                }
            }
            return maksimal > 0 ? maksimal : null;
        } catch (Exception e) {
            log.debug("Could not parse courier ETD '{}': {}", etd, e.getMessage());
            return null;
        }
    }

    private boolean akanGratis(BigDecimal subtotal) {
        int threshold = settingService.getInt("shipping.gratis-ongkir-minimal", 0);
        return threshold > 0 && subtotal != null
                && subtotal.compareTo(BigDecimal.valueOf(threshold)) >= 0;
    }

    /** Saat ongkir ditanggung toko, daftar kurir tetap dikirim ke pembeli dengan tarif 0. */
    private List<PilihanOngkirResponse> opsiDenganTarifGratis(List<PilihanOngkirResponse> opsi) {
        if (opsi == null || opsi.isEmpty()) {
            return opsiDariPengaturanKurir();
        }
        return opsi.stream()
                .map(o -> PilihanOngkirResponse.builder()
                        .kurirCode(o.getKurirCode())
                        .kurirName(o.getKurirName())
                        .layanan(o.getLayanan())
                        .deskripsi(o.getDeskripsi())
                        .tarif(BigDecimal.ZERO)
                        .estimasiHari(o.getEstimasiHari())
                        .build())
                .toList();
    }

    private List<PilihanOngkirResponse> opsiDariPengaturanKurir() {
        List<String> kurir = settingService.getList("shipping.rajaongkir-kurir");
        if (kurir == null || kurir.isEmpty()) {
            return List.of();
        }
        return kurir.stream()
                .map(k -> PilihanOngkirResponse.builder()
                        .kurirCode(k)
                        .kurirName(namaKurir(k))
                        .layanan("REG")
                        .deskripsi("Ongkir ditanggung toko")
                        .tarif(BigDecimal.ZERO)
                        .build())
                .toList();
    }

    private static String namaKurir(String kode) {
        if (kode == null) {
            return "";
        }
        return switch (kode.trim().toLowerCase()) {
            case "jne" -> "JNE";
            case "tiki" -> "TIKI";
            case "jnt", "j&t" -> "J&T";
            case "pos" -> "POS";
            case "sicepat" -> "SiCepat";
            default -> kode.trim().toUpperCase();
        };
    }

    private static boolean kosong(String nilai) {
        return nilai == null || nilai.isBlank();
    }

    private static boolean sama(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
