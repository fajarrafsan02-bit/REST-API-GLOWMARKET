package com.projekfajar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projekfajar.models.LoginOtp;
import com.projekfajar.models.User;

@Repository
public interface LoginOtpRepository extends JpaRepository<LoginOtp, Long>{
    Optional<LoginOtp> findByOtpAndUsedFalse(String token);
    
    List<LoginOtp> findByUserAndUsedFalse(User user);
}
