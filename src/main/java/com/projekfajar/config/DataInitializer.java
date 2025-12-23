package com.projekfajar.config;

import com.projekfajar.models.Produk;
import com.projekfajar.models.Role;
import com.projekfajar.models.StatusProduk;
import com.projekfajar.models.TerjualProduk;
import com.projekfajar.models.User;
import com.projekfajar.repository.ProdukRepository;
import com.projekfajar.repository.TerjualProdukRepository;
import com.projekfajar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
        private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final ProdukRepository produkRepository;
        private final TerjualProdukRepository terjualProdukRepository;

        @Override
        public void run(String... args) {
                initializeAdmin();
                initializeProduk();
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
                                .terferifikasi(true)
                                .createdAt(LocalDateTime.now())
                                .build();

                userRepository.save(admin);
                logger.info("Admin user created successfully:");
                logger.info("Email: {}", adminEmail);
                logger.info("Password: Admin123");
                logger.info("Role: ADMIN");
        }

        private void initializeProduk() {
                // Check if products already exist
                if (produkRepository.count() > 0) {
                        logger.info("Products already exist, skipping initialization");
                        return;
                }

                logger.info("Initializing products...");

                createProduk("Cincin Emas Elegan", 3850000.0, 3.2, 18, 12,
                                "https://images.unsplash.com/photo-1601121141461-9d6647bca1ed?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Premium", 7850000.0, 6.5, 22, 5,
                                "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Emas Wanita", 5650000.0, 5.1, 18, 8,
                                "https://images.unsplash.com/photo-1584302179602-e4c3d3fd629d?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Minimalis", 2450000.0, 2.1, 17, 20,
                                "https://images.unsplash.com/photo-1617038260897-41a1f14a8ca0?auto=format&fit=crop&w=900&q=80");

                createProduk("Liontin Emas Mewah", 3250000.0, 2.8, 24, 6,
                                "https://images.unsplash.com/photo-1599643477877-530eb83abc8e?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Berlian", 9250000.0, 4.5, 18, 4,
                                "https://images.unsplash.com/photo-1596944924616-7b38e7cfac36?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Emas Luxury", 10500000.0, 7.8, 22, 3,
                                "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Berlian", 6850000.0, 3.6, 18, 7,
                                "https://images.unsplash.com/photo-1602751584552-8ba73aad10e1?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Layer", 8950000.0, 6.9, 22, 5,
                                "https://ayugold.com/cdn/shop/products/b28b34a4-3eaf-45cb-9497-dca54745b66c-ayu-mini-solitaire-tapered-ring-8k.jpg?v=1706000792&width=1946");

                createProduk("Cincin Emas Couple", 4550000.0, 3.9, 18, 10,
                                "https://www.juenejewelry.com/cdn/shop/files/cincin-emas-7k-ciara-gold-ring-garden-collection-juene-jewelry-765083.jpg?v=1733968683&width=2048");

                createProduk("Gelang Tangan Emas Putih", 6750000.0, 5.5, 18, 9,
                                "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Mata Satu", 5250000.0, 4.2, 22, 7,
                                "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Butterfly", 7250000.0, 5.8, 18, 6,
                                "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Panjang", 4850000.0, 3.8, 18, 11,
                                "https://images.unsplash.com/photo-1630019852942-f89202989a59?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Engagement", 11500000.0, 5.5, 24, 4,
                                "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Kaki Emas", 3850000.0, 3.3, 18, 8,
                                "https://images.unsplash.com/photo-1611085583191-a3b181a88401?auto=format&fit=crop&w=900&q=80");

                createProduk("Kalung Emas Mutiara", 9850000.0, 7.2, 22, 5,
                                "https://images.unsplash.com/photo-1599707367072-cd6ada2bc375?auto=format&fit=crop&w=900&q=80");

                createProduk("Cincin Emas Eternity", 8250000.0, 4.8, 18, 6,
                                "https://images.unsplash.com/photo-1603561596112-0a132b757442?auto=format&fit=crop&w=900&q=80");

                createProduk("Anting Emas Tusuk", 2950000.0, 2.5, 18, 15,
                                "https://images.unsplash.com/photo-1635767798099-3b5bbf6e7461?auto=format&fit=crop&w=900&q=80");

                createProduk("Gelang Emas Chain", 7450000.0, 6.2, 22, 7,
                                "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?auto=format&fit=crop&w=900&q=80");

                logger.info("Successfully initialized 20 products");
        }

        private void createProduk(String nama, Double harga, Double beratGram, Integer karatEmas,
                        Integer stock, String gambar) {
                Produk produk = Produk.builder()
                                .nama(nama)
                                .harga(harga)
                                .beratGram(beratGram)
                                .karatEmas(karatEmas)
                                .stock(stock)
                                .gambar(gambar)
                                .status(StatusProduk.TERSEDIA)
                                .createdAt(LocalDateTime.now())
                                .build();

                Produk saved = produkRepository.save(produk);

                // Create terjual_produk entry with initial value 0
                TerjualProduk terjualProduk = TerjualProduk.builder()
                                .produk(saved)
                                .terjual(0)
                                .build();
                terjualProdukRepository.save(terjualProduk);

                logger.info("Created product: {} ({}K, {} gram, stock: {})",
                                nama, karatEmas, beratGram, stock);
        }
}
