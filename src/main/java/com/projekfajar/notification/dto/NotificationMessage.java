package com.projekfajar.notification.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
    private Long id;
    private String type; // NEW_CUSTOMER, NEW_ORDER, LOW_STOCK
    private String title;
    private String message;
    private Long userId;
    private Long paymentId;
    private Long produkId;
    private LocalDateTime timestamp;
    private Boolean isRead;
    private LocalDateTime readAt;
}
