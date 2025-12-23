package com.projekfajar.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.projekfajar.exception.EmailSendException;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    public void sendOtp(String email, String otp) {
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            logger.error("Email address is null or empty");
            throw new IllegalArgumentException("Email address tidak boleh kosong");
        }

        if (otp == null || otp.trim().isEmpty()) {
            logger.error("OTP is null or empty");
            throw new IllegalArgumentException("Kode OTP tidak boleh kosong");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            logger.error("Invalid email format: {}", email);
            throw new IllegalArgumentException("Format email tidak valid");
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Token Login Admin - Fajar Gold");

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
                                        Fajar Gold Admin
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
                                    © 2025 Fajar Gold. All rights reserved.<br>
                                    Sistem otomatis - jangan balas email ini.
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """, otp);

            helper.setText(htmlContent, true); // true = HTML

            javaMailSender.send(message);
            logger.info("OTP email HTML terkirim ke: {}", email);
        } catch (MailException e) {
            logger.error("Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            throw new EmailSendException("Gagal mengirim email. Silakan periksa koneksi internet Anda.", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending OTP email: {}", e.getMessage(), e);
            throw new EmailSendException("Terjadi kesalahan saat mengirim email: " + e.getMessage(), e);
        }
    }
    
    public void sendContactEmail(String senderName, String senderEmail, String senderPhone, String message) {
        // Validate inputs
        if (senderName == null || senderName.trim().isEmpty()) {
            logger.error("Sender name is null or empty");
            throw new IllegalArgumentException("Nama tidak boleh kosong");
        }
        
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            logger.error("Sender email is null or empty");
            throw new IllegalArgumentException("Email tidak boleh kosong");
        }
        
        if (!senderEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            logger.error("Invalid email format: {}", senderEmail);
            throw new IllegalArgumentException("Format email tidak valid");
        }
        
        if (message == null || message.trim().isEmpty()) {
            logger.error("Message is null or empty");
            throw new IllegalArgumentException("Pesan tidak boleh kosong");
        }
        
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            // Send to admin email (fajar.rafsan02@gmail.com)
            helper.setTo("fajar.rafsan02@gmail.com");
            helper.setSubject("Pesan Kontak Baru dari " + senderName);
            helper.setReplyTo(senderEmail); // Set reply-to as sender's email
            
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
                                        📧 Pesan Kontak Baru
                                    </h1>
                                    <p style="color:#cccccc; margin:10px 0 0; font-size:16px;">
                                        Fajar Gold - Contact Form
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
                                    © 2025 Fajar Gold. All rights reserved.<br>
                                    Email ini dikirim otomatis dari form kontak website.
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """, 
                    senderName, 
                    senderEmail, 
                    senderEmail, 
                    senderPhone,
                    message,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );
            
            helper.setText(htmlContent, true); // true = HTML
            
            javaMailSender.send(mimeMessage);
            logger.info("Contact email sent successfully from: {} ({})", senderName, senderEmail);
            
        } catch (MailException e) {
            logger.error("Failed to send contact email: {}", e.getMessage(), e);
            throw new EmailSendException("Gagal mengirim email. Silakan coba lagi.", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending contact email: {}", e.getMessage(), e);
            throw new EmailSendException("Terjadi kesalahan saat mengirim email: " + e.getMessage(), e);
        }
    }
}
