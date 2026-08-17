package com.projekfajar.chat.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.ongkir.model.Ongkir;
import com.projekfajar.ongkir.repository.OngkirRepository;
import com.projekfajar.ongkir.service.OngkirService;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.user.model.User;
import com.projekfajar.util.RupiahFormatter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Menyusun balasan otomatis dari data toko sendiri.
 *
 * Prinsipnya: bot tidak pernah menebak. Setiap angka yang disebutkan — harga,
 * stok, tarif ongkir, nomor resi — dibaca langsung dari database. Bila
 * pertanyaannya di luar jangkauan, ia mengembalikan Optional.empty() supaya
 * pemanggil bisa meneruskan pertanyaan itu ke admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotService {

    private final PesananRepository pesananRepository;
    private final OngkirRepository ongkirRepository;
    private final ProdukRepository produkRepository;
    private final SettingService settingService;

    /** Kata yang terlalu umum untuk dijadikan kata kunci pencarian produk. */
    private static final List<String> KATA_UMUM = List.of(
            "harga", "berapa", "stok", "ada", "ready", "tersedia", "apakah", "apa",
            "yang", "untuk", "dari", "dengan", "saya", "kak", "min", "mau", "beli",
            "punya", "masih", "itu", "ini", "dan", "atau", "nya", "kah", "gak",
            "tidak", "bisa", "boleh", "tolong", "info", "mohon");

    @Transactional(readOnly = true)
    public Optional<String> susunBalasan(User pelanggan, String pesan) {
        Long pelangganId = pelanggan != null ? pelanggan.getId() : null;

        if (pesan == null || pesan.isBlank()) {
            log.info("Bot reply skipped: empty question from user {}", pelangganId);
            return Optional.empty();
        }

        log.info("Composing bot reply for user {}, question: \"{}\"", pelangganId, ringkas(pesan));

        String teks = pesan.toLowerCase(new Locale("id", "ID")).trim();

        // Urutan diperiksa dari yang paling spesifik ke yang paling umum
        if (mengandung(teks, "resi", "pesanan saya", "orderan", "sampai mana", "status pesanan",
                "pesanan sudah", "dikirim belum", "sudah dikirim")) {
            log.info("Bot intent matched for user {}: ORDER_STATUS", pelangganId);
            return Optional.of(jawabStatusPesanan(pelanggan));
        }

        if (mengandung(teks, "ongkir", "ongkos kirim", "biaya kirim", "kirim ke")) {
            log.info("Bot intent matched for user {}: SHIPPING_COST", pelangganId);
            return Optional.of(jawabOngkir(teks));
        }

        if (mengandung(teks, "alamat", "lokasi", "jam buka", "jam berapa", "buka jam",
                "telepon", "nomor wa", "whatsapp", "kontak", "hubungi")) {
            log.info("Bot intent matched for user {}: STORE_INFO", pelangganId);
            return Optional.of(jawabInfoToko());
        }

        if (mengandung(teks, "harga", "stok", "ready", "tersedia", "ada gak", "ada ga",
                "masih ada")) {
            log.info("Bot intent matched for user {}: PRODUCT_PRICE_STOCK", pelangganId);
            return jawabProduk(teks);
        }

        if (mengandung(teks, "halo", "hai", "hallo", "assalam", "permisi",
                "selamat pagi", "selamat siang", "selamat sore", "selamat malam")) {
            log.info("Bot intent matched for user {}: GREETING", pelangganId);
            return Optional.of(jawabSapaan(pelanggan));
        }

        log.info("No bot intent matched for user {}, falling back to admin, question: \"{}\"",
                pelangganId, ringkas(pesan));
        return Optional.empty();
    }

    /** Potongan pendek pertanyaan untuk log, supaya isi percakapan tidak tercatat penuh. */
    private String ringkas(String pesan) {
        String satuBaris = pesan.replaceAll("\\s+", " ").trim();
        return satuBaris.length() <= 60 ? satuBaris : satuBaris.substring(0, 60) + "...";
    }

    private boolean mengandung(String teks, String... kunci) {
        for (String k : kunci) {
            if (teks.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private String jawabStatusPesanan(User pelanggan) {
        List<Pesanan> pesananList = pesananRepository.findByUserId(pelanggan.getId());

        if (pesananList.isEmpty()) {
            return "Saya belum menemukan pesanan atas nama akun Anda. "
                    + "Bila baru saja memesan, mohon tunggu sebentar sampai pembayarannya "
                    + "selesai diproses.";
        }

        Pesanan terbaru = pesananList.stream()
                .max(Comparator.comparing(Pesanan::getCreatedAt))
                .orElseThrow();

        StringBuilder jawaban = new StringBuilder()
                .append("Pesanan terakhir Anda ")
                .append(terbaru.getNomorPesanan())
                .append(" berstatus ")
                .append(statusManusiawi(terbaru.getStatus()))
                .append(".");

        if (terbaru.getStatus() == OrderStatus.DIKIRIM && terbaru.getNomorResi() != null) {
            jawaban.append(" Nomor resinya ").append(terbaru.getNomorResi()).append(".");
        }

        if (terbaru.getStatus() == OrderStatus.PENDING) {
            jawaban.append(" Pesanan akan kami proses setelah pembayaran diterima.");
        }

        return jawaban.toString();
    }

    private String statusManusiawi(OrderStatus status) {
        return switch (status) {
            case PENDING -> "menunggu pembayaran";
            case DIKEMAS -> "sedang disiapkan";
            case DIKIRIM -> "sedang dikirim";
            case SELESAI -> "selesai";
            case DIBATALKAN -> "dibatalkan";
            case DIKEMBALIKAN -> "dikembalikan";
        };
    }

    private String jawabOngkir(String teks) {
        Optional<String> provinsi = OngkirService.PROVINSI_SEED.stream()
                .filter(p -> teks.contains(p.toLowerCase(new Locale("id", "ID"))))
                .findFirst();

        if (provinsi.isEmpty()) {
            return "Boleh sebutkan provinsi tujuannya? Contoh: \"ongkir ke Jawa Timur\". "
                    + "Nanti saya cek tarifnya.";
        }

        Ongkir tarif = ongkirRepository.findByProvinsi(provinsi.get()).orElse(null);

        if (tarif == null) {
            return "Tarif untuk " + provinsi.get() + " belum diatur. "
                    + "Admin akan mengonfirmasi ongkirnya kepada Anda.";
        }

        if (tarif.getTarif().signum() == 0) {
            return "Pengiriman ke " + provinsi.get() + " saat ini gratis ongkir"
                    + estimasi(tarif) + ".";
        }

        return "Ongkir ke " + provinsi.get() + " sebesar "
                + RupiahFormatter.format(tarif.getTarif()) + estimasi(tarif) + ".";
    }

    private String estimasi(Ongkir tarif) {
        return tarif.getEstimasiHari() != null
                ? ", estimasi " + tarif.getEstimasiHari() + " hari"
                : "";
    }

    private String jawabInfoToko() {
        String nama = nilai("store.name", "Toko kami");
        String alamat = nilai("store.address", null);
        String telepon = nilai("store.phone", null);
        String wa = nilai("store.whatsapp", null);
        String email = nilai("store.email", null);

        StringBuilder jawaban = new StringBuilder(nama).append(":");

        if (alamat != null) {
            jawaban.append("\nAlamat: ").append(alamat);
        }
        if (telepon != null) {
            jawaban.append("\nTelepon: ").append(telepon);
        }
        if (wa != null) {
            jawaban.append("\nWhatsApp: ").append(wa);
        }
        if (email != null) {
            jawaban.append("\nEmail: ").append(email);
        }

        jawaban.append("\nJam operasional: Senin–Minggu, 09.00–18.00 WIB.");
        return jawaban.toString();
    }

    private String nilai(String kunci, String bawaan) {
        String isi = settingService.getValue(kunci);
        return isi != null && !isi.isBlank() ? isi : bawaan;
    }

    private Optional<String> jawabProduk(String teks) {
        // Ambil kata yang mungkin nama produk, buang kata umum
        List<String> kandidat = List.of(teks.split("[^a-zA-Z0-9]+")).stream()
                .filter(kata -> kata.length() >= 4)
                .filter(kata -> !KATA_UMUM.contains(kata))
                .toList();

        for (String kata : kandidat) {
            List<Produk> hasil = produkRepository
                    .findByNamaContainingIgnoreCaseAndDeletedFalse(kata);

            if (!hasil.isEmpty()) {
                log.debug("Product keyword '{}' matched {} products", kata, hasil.size());
                return Optional.of(rangkumProduk(hasil));
            }
        }

        // Pertanyaan soal harga/stok tapi produknya tidak dikenali
        log.info("No product recognized from {} keyword candidates, asking customer to clarify",
                kandidat.size());
        return Optional.of("Boleh sebutkan nama produknya lebih lengkap? "
                + "Contoh: \"harga cincin emas\" atau \"stok kalung\".");
    }

    private String rangkumProduk(List<Produk> hasil) {
        StringBuilder jawaban = new StringBuilder("Yang saya temukan:");

        hasil.stream().limit(3).forEach(produk -> {
            jawaban.append("\n• ")
                    .append(produk.getNama())
                    .append(" — ")
                    .append(RupiahFormatter.format(produk.getHarga()));

            int stok = produk.getStock() != null ? produk.getStock() : 0;
            jawaban.append(stok > 0 ? " (stok " + stok + ")" : " (stok habis)");
        });

        if (hasil.size() > 3) {
            jawaban.append("\n\nMasih ada ").append(hasil.size() - 3)
                    .append(" produk lain, silakan lihat di halaman Katalog.");
        }

        return jawaban.toString();
    }

    private String jawabSapaan(User pelanggan) {
        String sapaan = pelanggan.getNamaLengkap() != null
                ? "Halo " + pelanggan.getNamaLengkap() + "!"
                : "Halo!";

        return sapaan + " Saya asisten otomatis " + nilai("store.name", "toko kami")
                + ". Saya bisa membantu soal:\n"
                + "• Status pesanan dan nomor resi\n"
                + "• Tarif ongkir ke provinsi tujuan\n"
                + "• Harga dan ketersediaan produk\n"
                + "• Alamat, jam buka, dan kontak toko\n\n"
                + "Silakan tanyakan, atau tunggu admin bila pertanyaannya di luar itu.";
    }
}
