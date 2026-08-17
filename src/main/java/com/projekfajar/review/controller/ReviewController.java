package com.projekfajar.review.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.review.dto.ReviewRequest;
import com.projekfajar.review.dto.ReviewResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.review.service.ReviewService;
import com.projekfajar.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {
    
    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("POST /api/reviews rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            log.info("POST /api/reviews userId={} productId={} orderId={} rating={}",
                    user.getId(), request.getProdukId(), request.getPesananId(), request.getRating());
            ReviewResponse review = reviewService.createReview(user.getId(), request);
            
            log.info("Review created: reviewId={} productId={} rating={}",
                    review.getId(), review.getProdukId(), review.getRating());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review berhasil ditambahkan",
                    "data", review));
                    
        } catch (Exception e) {
            log.error("Failed to create review productId={} orderId={}: {}",
                    request.getProdukId(), request.getPesananId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/produk/{produkId}")
    public ResponseEntity<Map<String, Object>> getReviewsByProduk(@PathVariable Long produkId) {
        try {
            log.info("GET /api/reviews/produk/{}", produkId);
            List<ReviewResponse> reviews = reviewService.getReviewsByProduk(produkId);
            
            log.info("Returned {} reviews for productId={}", reviews.size(), produkId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data review berhasil diambil",
                    "data", reviews));
                    
        } catch (Exception e) {
            log.error("Failed to fetch reviews for productId={}: {}", produkId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getMyReviews(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/reviews/user rejected: unauthenticated request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            log.info("GET /api/reviews/user for userId={}", user.getId());
            List<ReviewResponse> reviews = reviewService.getReviewsByUser(user.getId());
            
            log.info("Returned {} reviews for userId={}", reviews.size(), user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data review berhasil diambil",
                    "data", reviews));
                    
        } catch (Exception e) {
            log.error("Failed to fetch user reviews: {}", e.getMessage(), e);
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
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                log.warn("GET /api/reviews/check/{}/{} rejected: unauthenticated request", produkId, pesananId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }
            
            log.info("GET /api/reviews/check productId={} orderId={} userId={}",
                    produkId, pesananId, user.getId());
            boolean hasReviewed = reviewService.hasUserReviewed(user.getId(), produkId, pesananId);
            
            log.info("Review check result: productId={} orderId={} hasReviewed={}",
                    produkId, pesananId, hasReviewed);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasReviewed", hasReviewed));
                    
        } catch (Exception e) {
            log.error("Failed to check review status productId={} orderId={}: {}",
                    produkId, pesananId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
