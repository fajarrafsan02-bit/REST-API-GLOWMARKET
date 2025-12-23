package com.projekfajar.services;

import com.projekfajar.DTO.ChatConversationResponse;
import com.projekfajar.DTO.ChatMessageRequest;
import com.projekfajar.DTO.ChatMessageResponse;
import com.projekfajar.models.ChatMessage;
import com.projekfajar.models.Role;
import com.projekfajar.models.User;
import com.projekfajar.repository.ChatMessageRepository;
import com.projekfajar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        logger.info("Sending message from user {} to user {}", senderId, request.getReceiverId());
        
        // Get sender
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Pengirim tidak ditemukan"));
        
        // Get receiver
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Penerima tidak ditemukan"));
        
        // Validate: User can only send to Admin, and Admin can send to User
        if (sender.getRole() == Role.USER && receiver.getRole() != Role.ADMIN) {
            throw new RuntimeException("User hanya bisa mengirim pesan ke Admin");
        }
        
        // Save message to database
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .senderRole(sender.getRole())
                .message(request.getMessage())
                .isRead(false)
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        logger.info("Message saved with ID: {}", savedMessage.getId());
        
        // Convert to response
        ChatMessageResponse response = convertToResponse(savedMessage, sender);
        
        // Send via WebSocket to receiver
        String destination = sender.getRole() == Role.ADMIN 
                ? "/topic/chat/user/" + request.getReceiverId()
                : "/topic/chat/admin/" + request.getReceiverId();
        
        messagingTemplate.convertAndSend(destination, response);
        logger.info("Message sent via WebSocket to {}", destination);
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long userId, Long adminId) {
        logger.info("Getting chat history between user {} and admin {}", userId, adminId);
        
        List<ChatMessage> messages = chatMessageRepository.findChatHistory(userId, adminId);
        
        return messages.stream()
                .map(this::convertToResponseWithUserInfo)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void markMessagesAsRead(Long receiverId, Long senderId) {
        logger.info("Marking messages as read for receiver {} from sender {}", receiverId, senderId);
        chatMessageRepository.markMessagesAsRead(receiverId, senderId);
    }
    
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return chatMessageRepository.countUnreadMessagesForUser(userId);
    }
    
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getAdminConversations(Long adminId) {
        logger.info("Getting all conversations for admin {}", adminId);
        
        List<Long> userIds = chatMessageRepository.findAllUserIdsChattedWithAdmin(adminId);
        List<ChatConversationResponse> conversations = new ArrayList<>();
        
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;
            
            ChatMessage lastMessage = chatMessageRepository.findLastMessage(userId, adminId);
            Long unreadCount = chatMessageRepository.countUnreadMessagesFromUser(adminId, userId);
            
            ChatConversationResponse conversation = ChatConversationResponse.builder()
                    .userId(userId)
                    .userName(user.getNamaLengkap())
                    .userEmail(user.getEmail())
                    .lastMessage(lastMessage != null ? lastMessage.getMessage() : "")
                    .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
                    .unreadCount(unreadCount)
                    .isOnline(false) // You can implement online status tracking
                    .build();
            
            conversations.add(conversation);
        }
        
        // Sort by last message time (newest first)
        conversations.sort((c1, c2) -> {
            if (c1.getLastMessageTime() == null) return 1;
            if (c2.getLastMessageTime() == null) return -1;
            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
        });
        
        return conversations;
    }
    
    private ChatMessageResponse convertToResponse(ChatMessage message, User sender) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .senderRole(message.getSenderRole())
                .message(message.getMessage())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .senderName(sender.getNamaLengkap())
                .senderEmail(sender.getEmail())
                .build();
    }
    
    private ChatMessageResponse convertToResponseWithUserInfo(ChatMessage message) {
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .senderRole(message.getSenderRole())
                .message(message.getMessage())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .senderName(sender != null ? sender.getNamaLengkap() : "Unknown")
                .senderEmail(sender != null ? sender.getEmail() : "")
                .build();
    }
}
