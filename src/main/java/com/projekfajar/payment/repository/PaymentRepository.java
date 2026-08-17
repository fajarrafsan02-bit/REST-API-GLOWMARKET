package com.projekfajar.payment.repository;

import com.projekfajar.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByExternalId(String externalId);
    
    Optional<Payment> findByXenditInvoiceId(String xenditInvoiceId);
    
    List<Payment> findByUserId(Long userId);
    
    List<Payment> findByStatus(String status);
    
    List<Payment> findByUserIdAndStatus(Long userId, String status);

    /** Invoice yang masih PENDING padahal masa berlakunya sudah lewat. */
    List<Payment> findByStatusAndExpiredAtBefore(String status, LocalDateTime batasWaktu);
}
