package com.projekfajar.wishlist.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projekfajar.wishlist.dto.WishlistResponse;
import com.projekfajar.user.model.User;
import com.projekfajar.wishlist.service.WishlistService;
import com.projekfajar.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishlistController {

    private final WishlistService wishlistService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWishlist(Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            List<WishlistResponse> wishlist = wishlistService.getWishlistByUser(user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data wishlist berhasil diambil",
                    "data", wishlist));
        } catch (Exception e) {
            log.error("Error getting wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Gagal mengambil data wishlist"));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addToWishlist(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            Long produkId = request.get("produkId");
            if (produkId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Produk ID tidak boleh kosong"));
            }

            WishlistResponse wishlist = wishlistService.addToWishlist(user.getId(), produkId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Produk berhasil ditambahkan ke wishlist",
                            "data", wishlist));
        } catch (RuntimeException e) {
            log.error("Error adding to wishlist: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding to wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Gagal menambahkan ke wishlist"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeFromWishlist(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            wishlistService.removeFromWishlist(user.getId(), id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produk berhasil dihapus dari wishlist"));
        } catch (Exception e) {
            log.error("Error removing from wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/check/{produkId}")
    public ResponseEntity<Map<String, Object>> checkWishlist(
            @PathVariable Long produkId,
            Authentication authentication) {
        try {
            User user = securityUtils.getCurrentUser(authentication);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Silakan login terlebih dahulu"));
            }

            boolean isInWishlist = wishlistService.isInWishlist(user.getId(), produkId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isInWishlist", isInWishlist));
        } catch (Exception e) {
            log.error("Error checking wishlist: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Gagal mengecek wishlist"));
        }
    }
}
