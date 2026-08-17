package com.projekfajar.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projekfajar.auth.model.RefreshToken;
import com.projekfajar.user.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select r from RefreshToken r join fetch r.user where r.tokenHash = :hash and r.revoked = false")
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
    int revokeAllByUser(@Param("user") User user);
}
