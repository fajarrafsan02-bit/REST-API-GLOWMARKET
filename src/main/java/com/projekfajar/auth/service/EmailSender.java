package com.projekfajar.auth.service;

/**
 * Jalur pengiriman email, dipisahkan dari penyusunan isinya.
 *
 * Dibutuhkan karena lingkungan produksi tidak selalu mengizinkan SMTP: Render,
 * Vercel, dan sebagian besar PaaS memblokir port 25/465/587 untuk menekan
 * penyalahgunaan spam, sehingga koneksi ke smtp.gmail.com menggantung lalu
 * gagal. Dengan antarmuka ini, EmailService cukup menyusun HTML dan tidak
 * perlu tahu apakah pesannya berangkat lewat SMTP atau HTTP API.
 */
public interface EmailSender {

    /**
     * @param tujuan alamat penerima
     * @param subjek judul email
     * @param htmlBody isi email dalam format HTML
     * @param replyTo alamat balasan, boleh null bila tidak diperlukan
     */
    void kirim(String tujuan, String subjek, String htmlBody, String replyTo);

    default void kirim(String tujuan, String subjek, String htmlBody) {
        kirim(tujuan, subjek, htmlBody, null);
    }
}
