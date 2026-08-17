package com.projekfajar.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.notification.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    Long countByReadFalse();
    
    List<Notification> findByProdukIdAndTypeAndCreatedAtAfter(Long produkId, String type, LocalDateTime createdAt);
    
    // Filter by notification TYPE (for role-based filtering)
    List<Notification> findTop50ByTypeInOrderByCreatedAtDesc(List<String> types);
    
    Long countByTypeInAndReadFalse(List<String> types);
    
    List<Notification> findByTypeInAndReadFalse(List<String> types);
    
    // User notifications: filter by TYPE and userId
    List<Notification> findByTypeInAndUserIdOrderByCreatedAtDesc(List<String> types, Long userId);
    
    Long countByTypeInAndUserIdAndReadFalse(List<String> types, Long userId);
    
    List<Notification> findByTypeInAndUserIdAndReadFalse(List<String> types, Long userId);
}
