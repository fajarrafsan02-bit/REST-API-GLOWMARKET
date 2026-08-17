package com.projekfajar.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import com.projekfajar.pesanan.model.AlamatSnapshot;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.pesanan.model.PesananItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // Jalur pengiriman ditentukan app.email.provider: SMTP untuk lokal,
    // HTTP API untuk produksi yang memblokir port SMTP.
    private final EmailSender emailSender;
    private final EmailTemplate template;

    /** Penerima pesan dari formulir kontak. */
    @Value("${app.email.admin:${app.email.from:}}")
    private String adminEmail;

    /**
     * Kode verifikasi kepemilikan email saat registrasi.
     *
     * Asinkron supaya pendaftaran tidak menunggu SMTP; kegagalannya tidak boleh
     * membatalkan akun yang sudah terbentuk.
     */
    @Async
    public void sendEmailVerification(String email, String nama, String kode) {
        log.info("Mengirim email verifikasi ke: {}", email);
        String isi = """
                <p>Halo <strong>%s</strong>,</p>
                <p>Masukkan kode berikut untuk memverifikasi alamat email Anda:</p>
                <div style="text-align:center; margin:28px 0;">
                  <div style="display:inline-block; padding:18px 36px; background:#fff9e6; border:3px dashed #f59e0b; border-radius:12px;">
                    <span style="font-size:40px; font-weight:bold; color:#d97706; letter-spacing:10px;">%s</span>
                  </div>
                </div>
                <p>Kode berlaku <strong>15 menit</strong> dan hanya bisa dipakai sekali.</p>
                <p style="color:#888; font-size:13px;">Verifikasi ini diperlukan agar bukti pembayaran
                dan pemberitahuan status pesanan dapat kami kirim ke email Anda.</p>
                """.formatted(template.escape(nama), template.escape(kode));

        kirim(email, "Kode Verifikasi Email - " + template.namaToko(),
                template.bungkus("Verifikasi Email", "Satu langkah lagi", isi));
    }

    /** Bukti pembayaran setelah invoice lunas. */
    public void sendPaymentSuccess(Pesanan pesanan) {
        log.info("Mengirim email bukti pembayaran untuk pesanan ID: {}", pesanan != null ? pesanan.getId() : "null");
        String isi = """
                <p>Halo <strong>%s</strong>,</p>
                <p>Pembayaran untuk pesanan <strong>%s</strong> sudah kami terima. Pesanan Anda
                akan segera kami siapkan.</p>
                %s
                %s
                """.formatted(
                template.escape(namaPenerima(pesanan)),
                template.escape(pesanan.getNomorPesanan()),
                tabelItem(pesanan),
                blokAlamat(pesanan));

        kirim(pesanan.getUser().getEmail(),
                "Pembayaran Berhasil - Pesanan " + pesanan.getNomorPesanan(),
                template.bungkus("Pembayaran Berhasil", "Terima kasih atas pesanan Anda", isi));
    }

    /** Pemberitahuan setiap kali status pesanan berpindah. */
    public void sendOrderStatus(Pesanan pesanan, OrderStatus status) {
        String judul;
        String subjudul;
        String pesan;

        switch (status) {
            case DIKEMAS -> {
                judul = "Pesanan Sedang Disiapkan";
                subjudul = "Pesanan " + pesanan.getNomorPesanan();
                pesan = "<p>Pesanan Anda sedang kami kemas dan akan segera dikirim.</p>";
            }
            case DIKIRIM -> {
                judul = "Pesanan Dikirim";
                subjudul = "Pesanan " + pesanan.getNomorPesanan();
                pesan = """
                        <p>Pesanan Anda sudah dalam perjalanan.</p>
                        <div style="margin:20px 0; padding:16px; background:#f8f9fa; border-radius:8px;">
                          <span style="color:#666; font-size:13px;">Nomor Resi</span><br>
                          <strong style="font-size:18px; letter-spacing:1px;">%s</strong>
                        </div>
                        """.formatted(template.escape(
                        pesanan.getNomorResi() != null ? pesanan.getNomorResi() : "-"));
            }
            case SELESAI -> {
                judul = "Pesanan Selesai";
                subjudul = "Pesanan " + pesanan.getNomorPesanan();
                pesan = """
                        <p>Pesanan Anda telah selesai. Terima kasih sudah berbelanja di %s.</p>
                        <p>Kami akan senang bila Anda berkenan memberi ulasan untuk produk yang dibeli.</p>
                        """.formatted(template.escape(template.namaToko()));
            }
            case DIBATALKAN -> {
                judul = "Pesanan Dibatalkan";
                subjudul = "Pesanan " + pesanan.getNomorPesanan();
                pesan = """
                        <p>Pesanan Anda dibatalkan dan stok produk telah kami lepas kembali.</p>
                        <p>Bila ini karena pembayaran melewati batas waktu, Anda dapat memesan ulang kapan saja.</p>
                        """;
            }
            default -> {
                log.debug("Status {} tidak mengirim email", status);
                return;
            }
        }

        String isi = "<p>Halo <strong>" + template.escape(namaPenerima(pesanan)) + "</strong>,</p>"
                + pesan
                + tabelItem(pesanan);

        kirim(pesanan.getUser().getEmail(),
                judul + " - Pesanan " + pesanan.getNomorPesanan(),
                template.bungkus(judul, subjudul, isi));
    }

    private String namaPenerima(Pesanan pesanan) {
        if (pesanan.getAlamatSnapshot() != null
                && pesanan.getAlamatSnapshot().getNamaLengkap() != null) {
            return pesanan.getAlamatSnapshot().getNamaLengkap();
        }
        return pesanan.getUser().getNamaLengkap();
    }

    private String tabelItem(Pesanan pesanan) {
        StringBuilder baris = new StringBuilder();

        for (PesananItem item : pesanan.getItems()) {
            String nama = item.getNamaProduk() != null
                    ? item.getNamaProduk()
                    : item.getProduk().getNama();

            baris.append("""
                    <tr>
                      <td style="padding:8px 0; border-bottom:1px solid #eee;">%s<br>
                        <span style="color:#888; font-size:12px;">%d x %s</span></td>
                      <td style="padding:8px 0; border-bottom:1px solid #eee; text-align:right;">%s</td>
                    </tr>
                    """.formatted(
                    template.escape(nama),
                    item.getQuantity(),
                    template.rupiah(item.getHargaSatuan()),
                    template.rupiah(item.getSubtotal())));
        }

        BigDecimal ongkir = pesanan.getOngkir() != null ? pesanan.getOngkir() : BigDecimal.ZERO;

        return """
                <table width="100%%" cellpadding="0" cellspacing="0" style="margin:20px 0; font-size:14px;">
                  %s
                  <tr>
                    <td style="padding:8px 0; color:#666;">Ongkos kirim</td>
                    <td style="padding:8px 0; text-align:right;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:12px 0; font-weight:bold; font-size:16px;">Total</td>
                    <td style="padding:12px 0; text-align:right; font-weight:bold; font-size:16px; color:#d97706;">%s</td>
                  </tr>
                </table>
                """.formatted(baris.toString(), template.rupiah(ongkir),
                template.rupiah(pesanan.getTotalHarga()));
    }

    private String blokAlamat(Pesanan pesanan) {
        AlamatSnapshot alamat = pesanan.getAlamatSnapshot();
        if (alamat == null || alamat.getAlamatLengkap() == null) {
            return "";
        }

        return """
                <div style="margin-top:20px; padding:16px; background:#f8f9fa; border-radius:8px;">
                  <span style="color:#666; font-size:13px;">Dikirim ke</span><br>
                  <strong>%s</strong> &middot; %s<br>
                  %s%s
                </div>
                """.formatted(
                template.escape(alamat.getNamaLengkap()),
                template.escape(alamat.getNomorTelepon()),
                template.escape(alamat.getAlamatLengkap()),
                alamat.getKota() != null ? ", " + template.escape(alamat.getKota()) : "");
    }

    /** Satu jalur pengiriman untuk semua email HTML. */
    private void kirim(String tujuan, String subjek, String htmlBody) {
        emailSender.kirim(tujuan, subjek, htmlBody);
    }

    /**
     * Kode OTP login admin.
     *
     * SENGAJA sinkron (tidak seperti sendEmailVerification): tanpa kode ini
     * admin tidak bisa masuk sama sekali, jadi respons HTTP tidak boleh
     * mengaku "OTP terkirim" sebelum SMTP benar-benar menerima pesannya.
     * Dulu method ini @Async, akibatnya frontend langsung menampilkan kolom
     * OTP padahal emailnya belum dikirim — dan kegagalan SMTP tidak pernah
     * sampai ke pemanggil karena sudah terjadi di thread lain.
     */
    public void sendOtp(String email, String otp) {
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            log.error("Email address is null or empty");
            throw new IllegalArgumentException("Email address tidak boleh kosong");
        }

        if (otp == null || otp.trim().isEmpty()) {
            log.error("OTP is null or empty");
            throw new IllegalArgumentException("Kode OTP tidak boleh kosong");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.error("Invalid email format: {}", email);
            throw new IllegalArgumentException("Format email tidak valid");
        }

        String htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Token Login Admin</title>
                    </head>
                    <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial, sans-serif;">
                        <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width:600px; margin:20px auto;">
                            <tr>
                                <td align="center" style="padding:40px 20px; background:#1a1a1a; border-radius:12px 12px 0 0;">
                                    <h1 style="color:#ffffff; margin:0; font-size:28px; font-weight:bold;">
                                        %s Admin
                                    </h1>
                                    <p style="color:#cccccc; margin:10px 0 0; font-size:16px;">
                                        Token Login Anda
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td bgcolor="#ffffff" style="padding:40px 30px; text-align:center; border-radius:0 0 12px 12px;">
                                    <p style="font-size:18px; color:#333333; margin:0 0 30px;">
                                        Gunakan token berikut untuk login ke panel admin:
                                    </p>

                                    <!-- OTP Box -->
                                    <div style="display:inline-block; padding:20px 40px; background:#fff9e6; border:3px dashed #f59e0b; border-radius:12px; margin:20px 0;">
                                        <h2 style="font-size:48px; font-weight:bold; color:#d97706; letter-spacing:12px; margin:0;">
                                            %s
                                        </h2>
                                    </div>

                                    <p style="font-size:16px; color:#666666; margin:30px 0 10px;">
                                        Token ini <strong>berlaku hanya 5 menit</strong> dan hanya bisa digunakan sekali.
                                    </p>
                                    <p style="font-size:14px; color:#999999; margin:20px 0 0;">
                                        Jika Anda tidak meminta token ini, abaikan email ini.
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td align="center" style="padding:30px; background:#1a1a1a; color:#888888; font-size:12px;">
                                    Â© 2025 %s. All rights reserved.<br>
                                    Sistem otomatis - jangan balas email ini.
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """, template.namaToko(), otp, template.namaToko());

        emailSender.kirim(email, "Token Login Admin - " + template.namaToko(), htmlContent);
        log.info("OTP email HTML terkirim ke: {}", email);
    }
    
    public void sendContactEmail(String senderName, String senderEmail, String senderPhone, String message) {
        // Validate inputs
        if (senderName == null || senderName.trim().isEmpty()) {
            log.error("Sender name is null or empty");
            throw new IllegalArgumentException("Nama tidak boleh kosong");
        }
        
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            log.error("Sender email is null or empty");
            throw new IllegalArgumentException("Email tidak boleh kosong");
        }
        
        if (!senderEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            log.error("Invalid email format: {}", senderEmail);
            throw new IllegalArgumentException("Format email tidak valid");
        }
        
        if (message == null || message.trim().isEmpty()) {
            log.error("Message is null or empty");
            throw new IllegalArgumentException("Pesan tidak boleh kosong");
        }
        
        String htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Pesan Kontak Baru</title>
                    </head>
                    <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial, sans-serif;">
                        <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width:600px; margin:20px auto;">
                            <tr>
                                <td align="center" style="padding:40px 20px; background:#1a1a1a; border-radius:12px 12px 0 0;">
                                    <h1 style="color:#ffffff; margin:0; font-size:28px; font-weight:bold;">
                                        ðŸ“§ Pesan Kontak Baru
                                    </h1>
                                    <p style="color:#cccccc; margin:10px 0 0; font-size:16px;">
                                        %s - Contact Form
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td bgcolor="#ffffff" style="padding:40px 30px; border-radius:0 0 12px 12px;">
                                    <!-- Sender Info -->
                                    <div style="background:#f8f9fa; padding:20px; border-radius:8px; margin-bottom:20px;">
                                        <h3 style="margin:0 0 15px; color:#333; font-size:18px;">Informasi Pengirim:</h3>
                                        <table width="100%%" cellpadding="5">
                                            <tr>
                                                <td width="120" style="color:#666; font-size:14px;"><strong>Nama:</strong></td>
                                                <td style="color:#333; font-size:14px;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="color:#666; font-size:14px;"><strong>Email:</strong></td>
                                                <td style="color:#333; font-size:14px;"><a href="mailto:%s" style="color:#f59e0b; text-decoration:none;">%s</a></td>
                                            </tr>
                                            <tr>
                                                <td style="color:#666; font-size:14px;"><strong>No. Telepon:</strong></td>
                                                <td style="color:#333; font-size:14px;">%s</td>
                                            </tr>
                                        </table>
                                    </div>
                                    
                                    <!-- Message Content -->
                                    <div style="background:#fff9e6; padding:20px; border-left:4px solid #f59e0b; border-radius:8px;">
                                        <h3 style="margin:0 0 15px; color:#333; font-size:18px;">Pesan:</h3>
                                        <p style="color:#333; font-size:14px; line-height:1.6; margin:0; white-space:pre-wrap;">%s</p>
                                    </div>
                                    
                                    <p style="font-size:12px; color:#999; margin:20px 0 0; text-align:center;">
                                        Dikirim pada: %s
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td align="center" style="padding:30px; background:#1a1a1a; color:#888888; font-size:12px;">
                                    Â© 2025 %s. All rights reserved.<br>
                                    Email ini dikirim otomatis dari form kontak website.
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """, 
                    template.namaToko(),
                    senderName, 
                    senderEmail, 
                    senderEmail, 
                    senderPhone,
                    message,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    template.namaToko()
            );

        // Balasan diarahkan ke pengirim asli supaya admin bisa langsung
        // membalas dari kotak masuknya.
        emailSender.kirim(adminEmail, "Pesan Kontak Baru dari " + senderName, htmlContent, senderEmail);
        log.info("Contact email sent successfully from: {} ({})", senderName, senderEmail);
    }
}
