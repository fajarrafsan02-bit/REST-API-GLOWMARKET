package com.projekfajar.wishlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.wishlist.model.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);
    Optional<Wishlist> findByUserIdAndProdukId(Long userId, Long produkId);
    boolean existsByUserIdAndProdukId(Long userId, Long produkId);

    /** Dipakai saat produk dihapus, agar tidak tertinggal di wishlist siapa pun. */
    void deleteByProdukId(Long produkId);
}
