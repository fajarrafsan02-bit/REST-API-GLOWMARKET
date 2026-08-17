package com.projekfajar.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.auth.model.LoginOtp;
import com.projekfajar.user.model.User;

@Repository
public interface LoginOtpRepository extends JpaRepository<LoginOtp, Long>{
    Optional<LoginOtp> findByOtpAndUsedFalse(String token);
    
    List<LoginOtp> findByUserAndUsedFalse(User user);
}
