package com.projekfajar.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.DTO.ReviewRequest;
import com.projekfajar.DTO.ReviewResponse;
import com.projekfajar.models.OrderStatus;
import com.projekfajar.models.Pesanan;
import com.projekfajar.models.Produk;
import com.projekfajar.models.Review;
import com.projekfajar.models.User;
import com.projekfajar.repository.PesananRepository;
import com.projekfajar.repository.ProdukRepository;
import com.projekfajar.repository.ReviewRepository;
import com.projekfajar.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProdukRepository produkRepository;
    private final PesananRepository pesananRepository;
    
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        logger.info("Creating review for user {} on product {}", userId, request.getProdukId());
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        
        // Get produk
        Produk produk = produkRepository.findById(request.getProdukId())
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan"));
        
        // Get pesanan
        Pesanan pesanan = pesananRepository.findById(request.getPesananId())
                .orElseThrow(() -> new RuntimeException("Pesanan tidak ditemukan"));
        
        // Validate: pesanan must belong to this user
        if (!pesanan.getUser().getId().equals(userId)) {
            throw new RuntimeException("Pesanan tidak ditemukan");
        }
        
        // Validate: pesanan must be completed (SELESAI)
        if (pesanan.getStatus() != OrderStatus.SELESAI) {
            throw new RuntimeException("Hanya pesanan yang sudah selesai yang bisa direview");
        }
        
        // Validate: user hasn't reviewed this product in this order yet
        if (reviewRepository.findByUserIdAndProdukIdAndPesananId(userId, request.getProdukId(), request.getPesananId()).isPresent()) {
            throw new RuntimeException("Anda sudah memberikan review untuk produk ini");
        }
        
        // Validate: product must be in this order
        boolean productInOrder = pesanan.getItems().stream()
                .anyMatch(item -> item.getProduk().getId().equals(request.getProdukId()));
        
        if (!productInOrder) {
            throw new RuntimeException("Produk tidak ada dalam pesanan ini");
        }
        
        // Create review
        Review review = Review.builder()
                .user(user)
                .produk(produk)
                .pesanan(pesanan)
                .rating(request.getRating())
                .komentar(request.getKomentar())
                .createdAt(LocalDateTime.now())
                .build();
        
        Review saved = reviewRepository.save(review);
        logger.info("Review created successfully with id: {}", saved.getId());
        
        return convertToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduk(Long produkId) {
        return reviewRepository.findByProdukIdOrderByCreatedAtDesc(produkId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public boolean hasUserReviewed(Long userId, Long produkId, Long pesananId) {
        return reviewRepository.findByUserIdAndProdukIdAndPesananId(userId, produkId, pesananId).isPresent();
    }
    
    private ReviewResponse convertToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getNamaLengkap())
                .produkId(review.getProduk().getId())
                .namaProduk(review.getProduk().getNama())
                .pesananId(review.getPesanan().getId())
                .nomorPesanan(review.getPesanan().getNomorPesanan())
                .rating(review.getRating())
                .komentar(review.getKomentar())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
