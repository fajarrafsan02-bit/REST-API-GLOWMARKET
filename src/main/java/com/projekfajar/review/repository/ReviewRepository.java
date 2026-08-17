package com.projekfajar.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.projekfajar.review.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Get all reviews for a product
    List<Review> findByProdukIdOrderByCreatedAtDesc(Long produkId);
    
    // Get all reviews by user
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Check if user already reviewed a product in specific order
    Optional<Review> findByUserIdAndProdukIdAndPesananId(Long userId, Long produkId, Long pesananId);
    
    // Check if user already reviewed any product in specific order
    boolean existsByUserIdAndPesananId(Long userId, Long pesananId);
    
    // Get average rating for a product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.produk.id = :produkId")
    Double getAverageRatingByProdukId(Long produkId);
    
    // Count reviews for a product
    Long countByProdukId(Long produkId);
}
