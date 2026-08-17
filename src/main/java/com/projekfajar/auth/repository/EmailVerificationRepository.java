package com.projekfajar.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.auth.model.EmailVerification;
import com.projekfajar.user.model.User;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /** Kode aktif terbaru milik seorang pengguna. */
    Optional<EmailVerification> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);
}
