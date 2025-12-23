package com.projekfajar.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.DTO.ContactRequest;
import com.projekfajar.services.EmailService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ContactController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    
    private final EmailService emailService;
    
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendContactEmail(@Valid @RequestBody ContactRequest request) {
        try {
            logger.info("Receiving contact email from: {} ({})", request.getNamaLengkap(), request.getEmail());
            
            emailService.sendContactEmail(
                    request.getNamaLengkap(),
                    request.getEmail(),
                    request.getNoTelepon(),
                    request.getPesan()
            );
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pesan Anda berhasil dikirim! Kami akan segera menghubungi Anda."));
                    
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid contact request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
                    
        } catch (Exception e) {
            logger.error("Error sending contact email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", 
                            "Gagal mengirim pesan. Silakan coba lagi atau hubungi kami melalui WhatsApp."));
        }
    }
}
