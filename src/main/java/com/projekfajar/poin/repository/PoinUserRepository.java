package com.projekfajar.poin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.poin.model.PoinUser;

public interface PoinUserRepository extends JpaRepository<PoinUser, Long> {

    Optional<PoinUser> findByUserId(Long userId);
}
