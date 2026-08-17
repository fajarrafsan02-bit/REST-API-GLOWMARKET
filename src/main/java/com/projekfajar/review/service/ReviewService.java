package com.projekfajar.review.service;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.review.dto.ReviewRequest;
import com.projekfajar.review.dto.ReviewResponse;
import com.projekfajar.pesanan.model.OrderStatus;
import com.projekfajar.pesanan.model.Pesanan;
import com.projekfajar.produk.model.Produk;
import com.projekfajar.review.model.Review;
import com.projekfajar.user.model.User;
import com.projekfajar.pesanan.repository.PesananRepository;
import com.projekfajar.produk.repository.ProdukRepository;
import com.projekfajar.review.repository.ReviewRepository;
import com.projekfajar.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProdukRepository produkRepository;
    private final PesananRepository pesananRepository;
    
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        log.info("Creating review userId={} productId={} orderId={} rating={}",
                userId, request.getProdukId(), request.getPesananId(), request.getRating());
        
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
        
        // Get produk
        Produk produk = produkRepository.findById(request.getProdukId())
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan"));
        
        // Get pesanan
        Pesanan pesanan = pesananRepository.findById(request.getPesananId())
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan"));
        
        // Validate: pesanan must belong to this user
        if (!pesanan.getUser().getId().equals(userId)) {
            log.warn("Review rejected: orderId={} does not belong to userId={}", request.getPesananId(), userId);
            throw new ResourceNotFoundException("Pesanan tidak ditemukan");
        }
        
        // Validate: pesanan must be completed (SELESAI)
        if (pesanan.getStatus() != OrderStatus.SELESAI) {
            log.warn("Review rejected: orderId={} status={} is not SELESAI",
                    request.getPesananId(), pesanan.getStatus());
            throw new BusinessException("Hanya pesanan yang sudah selesai yang bisa direview");
        }
        
        // Validate: user hasn't reviewed this product in this order yet
        if (reviewRepository.findByUserIdAndProdukIdAndPesananId(userId, request.getProdukId(), request.getPesananId()).isPresent()) {
            log.warn("Review rejected: userId={} already reviewed productId={} in orderId={}",
                    userId, request.getProdukId(), request.getPesananId());
            throw new BusinessException("Anda sudah memberikan review untuk produk ini");
        }
        
        // Validate: product must be in this order
        boolean productInOrder = pesanan.getItems().stream()
                .anyMatch(item -> item.getProduk().getId().equals(request.getProdukId()));
        
        if (!productInOrder) {
            log.warn("Review rejected: productId={} not part of orderId={}",
                    request.getProdukId(), request.getPesananId());
            throw new BusinessException("Produk tidak ada dalam pesanan ini");
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
        log.info("Review created: reviewId={} userId={} productId={} rating={}",
                saved.getId(), userId, request.getProdukId(), saved.getRating());
        
        return convertToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduk(Long produkId) {
        log.debug("Fetching reviews for productId={}", produkId);
        List<ReviewResponse> hasil = reviewRepository.findByProdukIdOrderByCreatedAtDesc(produkId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        log.debug("Found {} reviews for productId={}", hasil.size(), produkId);
        return hasil;
    }
    
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(Long userId) {
        log.debug("Fetching reviews for userId={}", userId);
        List<ReviewResponse> hasil = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        log.debug("Found {} reviews for userId={}", hasil.size(), userId);
        return hasil;
    }
    
    @Transactional(readOnly = true)
    public boolean hasUserReviewed(Long userId, Long produkId, Long pesananId) {
        boolean sudah = reviewRepository
                .findByUserIdAndProdukIdAndPesananId(userId, produkId, pesananId).isPresent();
        log.debug("Review check userId={} productId={} orderId={} hasReviewed={}",
                userId, produkId, pesananId, sudah);
        return sudah;
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
