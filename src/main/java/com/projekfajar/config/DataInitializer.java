package com.projekfajar.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.projekfajar.auth.model.Role;
import com.projekfajar.ongkir.service.OngkirService;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.produk.model.StatusProduk;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.settings.model.AppSetting;
import com.projekfajar.settings.repository.SettingRepository;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
        private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final ProdukRepository produkRepository;
        private final SettingRepository settingRepository;
        private final OngkirService ongkirService;

        @Override
        public void run(String... args) {
                logger.info("Running DataInitializer - Seeding Admin user only");
                initializeAdmin();
                // Data seeder lain (settings, produk, ongkir) dinonaktifkan sesuai permintaan
                // initializeSettings();
                // initializeProduk();
                // ongkirService.seedDefaults();
        }

        private void initializeAdmin() {
                String adminEmail = "fajar.rafsan02@gmail.com";

                // Check if admin already exists
                if (userRepository.findByEmail(adminEmail).isPresent()) {
                        logger.info("Admin user already exists, skipping initialization");
                        return;
                }

                // Create admin user
                User admin = User.builder()
                                .namaLengkap("Fajar Rafsan Tanjung")
                                .email(adminEmail)
                                .password(passwordEncoder.encode("Admin123"))
                                .noHp("081234567890")
                                .role(Role.ADMIN)
                                .terverifikasi(true)
                                .createdAt(LocalDateTime.now())
                                .build();

                userRepository.save(admin);
                logger.info("Admin user created successfully:");
                logger.info("Email: {}", adminEmail);
                logger.info("Password: Admin123");
                logger.info("Role: ADMIN");
        }

        private void initializeSettings() {
                if (settingRepository.count() > 0) {
                        logger.info("Settings already exist, skipping initialization");
                        return;
                }

                Map.ofEntries(
                        Map.entry("store.name", "GlowMarket"),
                        Map.entry("store.tagline", "Perhiasan Emas Asli Berkualitas"),
                        Map.entry("store.description", "Toko perhiasan emas terpercaya dengan berbagai koleksi cincin, kalung, gelang, dan anting emas asli untuk Anda."),
                        Map.entry("store.phone", "081234567890"),
                        Map.entry("store.email", "fajar.rafsan02@gmail.com"),
                        Map.entry("store.instagram", "fajargold"),
                        Map.entry("store.whatsapp", "6281234567890"),
                        Map.entry("store.address", "Jl. Mawar No. 12, Jakarta Selatan"),
                        Map.entry("store.logo", ""),
                        Map.entry("lowStock.threshold", "5"),
                        // Balasan otomatis chat saat admin sedang offline
                        Map.entry("chatbot.enabled", "true"),
                        Map.entry("chatbot.pesan_fallback",
                                "Terima kasih atas pesan Anda. Pertanyaan ini akan dijawab admin pada jam kerja. "
                                        + "Sementara itu saya bisa membantu soal status pesanan, ongkir, harga produk, dan info toko."),
                        // Kosong = semua metode aktif di akun Xendit ditampilkan
                        Map.entry("payment.methods", ""))
                        .forEach((key, value) -> {
                                AppSetting setting = AppSetting.builder()
                                                .key(key)
                                                .value(value)
                                                .updatedAt(LocalDateTime.now())
                                                .build();
                                settingRepository.save(setting);
                        });

                logger.info("Default settings initialized");
        }

        private void initializeProduk() {
                // Check if products already exist
                if (produkRepository.count() > 0) {
                        logger.info("Products already exist, skipping initialization");
                        return;
                }

                logger.info("Initializing products...");

                createProduk("Cincin Emas Elegan", 3850000L, 3.2, 18, 12,
                                "https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Premium", 7850000L, 6.5, 22, 5,
                                "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Emas Wanita", 5650000L, 5.1, 18, 8,
                                "https://images.unsplash.com/photo-1584302179602-e4c3d3fd629d?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Minimalis", 2450000L, 2.1, 17, 20,
                                "https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?auto=format&fit=crop&w=900&q=80");

                createProduk("Liontin Emas Mewah", 3250000L, 2.8, 24, 6,
                                "https://images.unsplash.com/photo-1599643477877-530eb83abc8e?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Berlian", 9250000L, 4.5, 18, 4,
                                "https://images.unsplash.com/photo-1596944924616-7b38e7cfac36?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Emas Luxury", 10500000L, 7.8, 22, 3,
                                "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Berlian", 6850000L, 3.6, 18, 7,
                                "https://images.unsplash.com/photo-1602751584552-8ba73aad10e1?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Layer", 8950000L, 6.9, 22, 5,
                                "https://ayugold.com/cdn/shop/products/b28b34a4-3eaf-45cb-9497-dca54745b66c-ayu-mini-solitaire-tapered-ring-8k.jpg?v=1706000792&width=1946");

                createProduk("Cincin Emas Couple", 4550000L, 3.9, 18, 10,
                                "https://www.juenejewelry.com/cdn/shop/files/cincin-emas-7k-ciara-gold-ring-garden-collection-juene-jewelry-765083.jpg?v=1733968683&width=2048");

                createProduk("Gelang Tangan Emas Putih", 6750000L, 5.5, 18, 9,
                                "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Mata Satu", 5250000L, 4.2, 22, 7,
                                "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Butterfly", 7250000L, 5.8, 18, 6,
                                "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Panjang", 4850000L, 3.8, 18, 11,
                                "https://images.unsplash.com/photo-1630019852942-f89202989a59?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Engagement", 11500000L, 5.5, 24, 4,
                                "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Kaki Emas", 3850000L, 3.3, 18, 8,
                                "https://images.unsplash.com/photo-1611085583191-a3b181a88401?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Mutiara", 9850000L, 7.2, 22, 5,
                                "https://images.unsplash.com/photo-1599707367072-cd6ada2bc375?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Eternity", 8250000L, 4.8, 18, 6,
                                "https://images.unsplash.com/photo-1603561596112-0a132b757442?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Tusuk", 2950000L, 2.5, 18, 15,
                                "https://images.unsplash.com/photo-1635767798099-3b5bbf6e7461?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Emas Chain", 7450000L, 6.2, 22, 7,
                                "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?auto=format&fit=crop&w=900&q=80");

                logger.info("Successfully initialized 20 products");
        }

        private void createProduk(String nama, long harga, Double beratGram, Integer karatEmas,
                        Integer stock, String gambar) {
                Produk produk = Produk.builder()
                                .nama(nama)
                                .harga(BigDecimal.valueOf(harga))
                                .beratGram(beratGram)
                                .karatEmas(karatEmas)
                                .stock(stock)
                                .gambar(gambar)
                                .status(StatusProduk.TERSEDIA)
                                .createdAt(LocalDateTime.now())
                                .build();

                produkRepository.save(produk);

                logger.info("Created product: {} ({}K, {} gram, stock: {})",
                                nama, karatEmas, beratGram, stock);
        }
}
