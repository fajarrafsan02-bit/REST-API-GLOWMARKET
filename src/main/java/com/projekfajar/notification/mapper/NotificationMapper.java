package com.projekfajar.notification.mapper;

import com.projekfajar.notification.dto.NotificationMessage;
import com.projekfajar.notification.model.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationMessage toMessage(Notification n) {
        return NotificationMessage.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .userId(n.getUserId())
                .paymentId(n.getPaymentId())
                .produkId(n.getProdukId())
                .timestamp(n.getCreatedAt())
                .isRead(n.getRead())
                .readAt(n.getReadAt())
                .build();
    }
}
