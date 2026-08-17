package com.projekfajar.auth.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.projekfajar.settings.service.SettingService;
import com.projekfajar.util.RupiahFormatter;

import lombok.RequiredArgsConstructor;

/**
 * Kerangka HTML bersama untuk semua email keluar.
 *
 * Sebelumnya tiap metode di EmailService menempelkan HTML lengkapnya sendiri dan
 * menulis nama toko secara hardcode. Sekarang identitas toko diambil dari
 * pengaturan admin, jadi email otomatis memakai nama toko yang sedang berlaku.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplate {

    private final SettingService settingService;

    public String namaToko() {
        String nama = settingService.getValue("store.name");
        return nama != null && !nama.isBlank() ? nama : "GlowMarket";
    }

    private String kontakToko() {
        String email = settingService.getValue("store.email");
        String telepon = settingService.getValue("store.phone");

        StringBuilder kontak = new StringBuilder();
        if (email != null && !email.isBlank()) {
            kontak.append(email);
        }
        if (telepon != null && !telepon.isBlank()) {
            if (kontak.length() > 0) {
                kontak.append(" &middot; ");
            }
            kontak.append(telepon);
        }
        return kontak.toString();
    }

    /** Format rupiah untuk isi email — sama persis dengan yang dipakai chatbot. */
    public String rupiah(BigDecimal nilai) {
        return RupiahFormatter.format(nilai);
    }

    /**
     * Membungkus isi email dengan header dan footer yang seragam.
     *
     * @param judul     tampil besar di header
     * @param subjudul  baris kecil di bawah judul (boleh null)
     * @param isiHtml   isi utama, sudah berupa HTML
     */
    public String bungkus(String judul, String subjudul, String isiHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0; padding:0; background-color:#f4f4f4; font-family:Arial, sans-serif;">
                  <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width:600px; margin:20px auto;">
                    <tr>
                      <td align="center" style="padding:36px 20px; background:#1a1a1a; border-radius:12px 12px 0 0;">
                        <h1 style="color:#ffffff; margin:0; font-size:24px; font-weight:bold;">%s</h1>
                        <p style="color:#cccccc; margin:8px 0 0; font-size:15px;">%s</p>
                      </td>
                    </tr>
                    <tr>
                      <td bgcolor="#ffffff" style="padding:32px 28px; border-radius:0 0 12px 12px; color:#333333; font-size:14px; line-height:1.7;">
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td align="center" style="padding:24px; color:#888888; font-size:12px;">
                        %s<br>%s<br>
                        Email otomatis &mdash; mohon tidak membalas pesan ini.
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(judul),
                subjudul != null ? escape(subjudul) : "",
                isiHtml,
                escape(namaToko()),
                kontakToko());
    }

    /** Mencegah nilai dari database merusak struktur HTML email. */
    public String escape(String teks) {
        if (teks == null) {
            return "";
        }
        return teks.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
