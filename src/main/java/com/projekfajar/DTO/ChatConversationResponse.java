package com.projekfajar.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
    private Boolean isOnline;
}
