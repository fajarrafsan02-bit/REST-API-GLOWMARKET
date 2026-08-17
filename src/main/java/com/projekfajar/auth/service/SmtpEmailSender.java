package com.projekfajar.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.projekfajar.exception.EmailSendException;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pengiriman lewat SMTP — dipakai saat pengembangan lokal.
 *
 * Aktif bila app.email.provider bernilai "smtp" (nilai bawaan), sehingga
 * perilaku di komputer pengembang tidak berubah sama sekali.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void kirim(String tujuan, String subjek, String htmlBody, String replyTo) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(tujuan);
            helper.setSubject(subjek);
            helper.setText(htmlBody, true);
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            javaMailSender.send(message);
            log.info("Email '{}' terkirim ke {} lewat SMTP", subjek, tujuan);
        } catch (MailException e) {
            log.error("SMTP gagal mengirim '{}' ke {}: {}", subjek, tujuan, e.getMessage());
            throw new EmailSendException("Gagal mengirim email ke " + tujuan, e);
        } catch (Exception e) {
            log.error("Kesalahan tak terduga saat mengirim email ke {}: {}", tujuan, e.getMessage(), e);
            throw new EmailSendException("Terjadi kesalahan saat mengirim email", e);
        }
    }
}
