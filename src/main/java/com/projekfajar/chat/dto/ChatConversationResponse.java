package com.projekfajar.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
