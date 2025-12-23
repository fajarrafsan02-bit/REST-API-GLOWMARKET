package com.projekfajar.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.DTO.ReviewRequest;
import com.projekfajar.DTO.ReviewResponse;
import com.projekfajar.models.User;
import com.projekfajar.repository.UserRepository;
import com.projekfajar.services.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            ReviewResponse review = reviewService.createReview(user.getId(), request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review berhasil ditambahkan",
                    "data", review));
                    
        } catch (Exception e) {
            logger.error("Error creating review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/produk/{produkId}")
    public ResponseEntity<Map<String, Object>> getReviewsByProduk(@PathVariable Long produkId) {
        try {
            List<ReviewResponse> reviews = reviewService.getReviewsByProduk(produkId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data review berhasil diambil",
                    "data", reviews));
                    
        } catch (Exception e) {
            logger.error("Error getting reviews by produk: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getMyReviews(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            List<ReviewResponse> reviews = reviewService.getReviewsByUser(user.getId());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data review berhasil diambil",
                    "data", reviews));
                    
        } catch (Exception e) {
            logger.error("Error getting user reviews: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/check/{produkId}/{pesananId}")
    public ResponseEntity<Map<String, Object>> checkIfReviewed(
            @PathVariable Long produkId,
            @PathVariable Long pesananId,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            
            boolean hasReviewed = reviewService.hasUserReviewed(user.getId(), produkId, pesananId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasReviewed", hasReviewed));
                    
        } catch (Exception e) {
            logger.error("Error checking review status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
