package com.projekfajar.chat.dto;

import com.projekfajar.auth.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Role senderRole;
    private String message;
    private Boolean isRead;

    /** true bila pesan ini balasan otomatis bot, bukan ketikan admin. */
    private Boolean dariBot;

    private LocalDateTime createdAt;
    
    // Additional info for display
    private String senderName;
    private String senderEmail;
}
