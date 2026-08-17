package com.projekfajar.chat.service;

import com.projekfajar.exception.ResourceNotFoundException;

import com.projekfajar.exception.BusinessException;

import com.projekfajar.chat.dto.ChatConversationResponse;
import com.projekfajar.chat.dto.ChatMessageRequest;
import com.projekfajar.chat.dto.ChatMessageResponse;
import com.projekfajar.chat.model.ChatMessage;
import com.projekfajar.auth.model.Role;
import com.projekfajar.user.model.User;
import com.projekfajar.chat.repository.ChatMessageRepository;
import com.projekfajar.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.settings.service.SettingService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineUserTracker onlineUserTracker;
    private final ChatBotService chatBotService;
    private final SettingService settingService;
    private final NotificationService notificationService;

    /** Jeda minimum antar pesan "belum bisa saya jawab" ke pelanggan yang sama. */
    private static final int JEDA_FALLBACK_MENIT = 5;
    
    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, ChatMessageRequest request) {
        log.info("Sending message from user {} to user {}", senderId, request.getReceiverId());
        
        // Get sender
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pengirim tidak ditemukan"));
        
        // Get receiver
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Penerima tidak ditemukan"));
        
        // Validate: User can only send to Admin, and Admin can send to User
        if (sender.getRole() == Role.USER && receiver.getRole() != Role.ADMIN) {
            throw new BusinessException("User hanya bisa mengirim pesan ke Admin");
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
        log.info("Message saved with ID: {}", savedMessage.getId());
        
        // Convert to response
        ChatMessageResponse response = convertToResponse(savedMessage, sender);
        
        // Send via WebSocket to receiver
        String destination = sender.getRole() == Role.ADMIN 
                ? "/topic/chat/user/" + request.getReceiverId()
                : "/topic/chat/admin/" + request.getReceiverId();
        
        messagingTemplate.convertAndSend(destination, response);
        log.info("Message sent via WebSocket to {}", destination);

        // Pesan pelanggan saat admin tidak online dijawab bot. Hanya dipicu pada
        // arah USER -> ADMIN, sehingga balasan bot (yang berperan ADMIN) tidak
        // mungkin memicu dirinya sendiri.
        if (sender.getRole() == Role.USER) {
            balasOtomatisBilaAdminOffline(sender, receiver, request.getMessage());
        }

        return response;
    }

    /**
     * Menjawab pelanggan ketika admin sedang tidak online.
     *
     * Kegagalan di sini tidak boleh menggagalkan pesan pelanggan — pesan aslinya
     * sudah tersimpan dan terkirim sebelum bagian ini dijalankan.
     */
    private void balasOtomatisBilaAdminOffline(User pelanggan, User admin, String pesan) {
        try {
            if (!"true".equalsIgnoreCase(settingService.getValue("chatbot.enabled"))) {
                return;
            }

            if (onlineUserTracker.isUserOnline(admin.getId())) {
                return;
            }

            Optional<String> balasan = chatBotService.susunBalasan(pelanggan, pesan);

            if (balasan.isEmpty()) {
                kirimFallback(pelanggan, admin, pesan);
                return;
            }

            kirimPesanBot(pelanggan, admin, balasan.get());
        } catch (Exception e) {
            log.error("Gagal menyusun balasan otomatis untuk user {}: {}",
                    pelanggan.getId(), e.getMessage(), e);
        }
    }

    /**
     * Pertanyaan di luar jangkauan bot: beri tahu pelanggan sekali saja dalam
     * rentang waktu tertentu, lalu pastikan admin tahu ada yang menunggu.
     */
    private void kirimFallback(User pelanggan, User admin, String pesanAsli) {
        ChatMessage balasanTerakhir = chatMessageRepository.findLastBotMessage(pelanggan.getId());

        boolean baruSajaDibalas = balasanTerakhir != null
                && balasanTerakhir.getCreatedAt() != null
                && balasanTerakhir.getCreatedAt()
                        .isAfter(LocalDateTime.now().minusMinutes(JEDA_FALLBACK_MENIT));

        if (!baruSajaDibalas) {
            String pesan = settingService.getValue("chatbot.pesan_fallback");

            if (pesan == null || pesan.isBlank()) {
                pesan = "Terima kasih atas pesan Anda. Pertanyaan ini akan dijawab admin "
                        + "pada jam kerja. Sementara itu saya bisa membantu soal status "
                        + "pesanan, ongkir, harga produk, dan info toko.";
            }

            kirimPesanBot(pelanggan, admin, pesan);
        }

        // Admin tetap diberi tahu walau pesan fallback ditahan agar tidak berulang
        try {
            notificationService.sendPertanyaanBelumTerjawab(pelanggan, pesanAsli);
        } catch (Exception e) {
            log.error("Gagal mengirim notifikasi pertanyaan pelanggan: {}", e.getMessage());
        }
    }

    private void kirimPesanBot(User pelanggan, User admin, String isi) {
        ChatMessage pesanBot = chatMessageRepository.save(ChatMessage.builder()
                .senderId(admin.getId())
                .receiverId(pelanggan.getId())
                .senderRole(Role.ADMIN)
                .message(isi)
                .isRead(false)
                .dariBot(true)
                .build());

        ChatMessageResponse response = convertToResponse(pesanBot, admin);

        messagingTemplate.convertAndSend("/topic/chat/user/" + pelanggan.getId(), response);
        log.info("Balasan bot terkirim ke user {}", pelanggan.getId());
    }

    /**
     * Mengirim pesan otomatis atas nama admin (ditandai dari bot) ke pelanggan.
     * Dipakai pemicu sistem seperti tracking kurir — pelanggan tidak perlu
     * menunggu admin mengetik manual.
     */
    public void kirimPesanOtomatisDariAdmin(User pelanggan, String isi) {
        if (pelanggan == null || isi == null || isi.isBlank()) {
            return;
        }

        List<User> daftarAdmin = userRepository.findByRole(Role.ADMIN);
        if (daftarAdmin.isEmpty()) {
            log.warn("Tidak ada admin untuk mengirim pesan otomatis ke user {}",
                    pelanggan.getId());
            return;
        }

        kirimPesanBot(pelanggan, daftarAdmin.get(0), isi);
    }
    
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long userId, Long adminId) {
        log.info("Getting chat history between user {} and admin {}", userId, adminId);
        
        List<ChatMessage> messages = chatMessageRepository.findChatHistory(userId, adminId);
        
        return messages.stream()
                .map(this::convertToResponseWithUserInfo)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void markMessagesAsRead(Long receiverId, Long senderId) {
        log.info("Marking messages as read for receiver {} from sender {}", receiverId, senderId);
        chatMessageRepository.markMessagesAsRead(receiverId, senderId);
    }
    
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return chatMessageRepository.countUnreadMessagesForUser(userId);
    }
    
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getAdminConversations(Long adminId) {
        log.info("Getting all conversations for admin {}", adminId);
        
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
                .dariBot(message.getDariBot())
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
                .dariBot(message.getDariBot())
                .createdAt(message.getCreatedAt())
                .senderName(sender != null ? sender.getNamaLengkap() : "Unknown")
                .senderEmail(sender != null ? sender.getEmail() : "")
                .build();
    }
}
