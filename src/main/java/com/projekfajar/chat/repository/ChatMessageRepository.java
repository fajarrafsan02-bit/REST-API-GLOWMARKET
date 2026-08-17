package com.projekfajar.chat.repository;

import com.projekfajar.chat.model.ChatMessage;
import com.projekfajar.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

       // Get chat history between user and admin
       @Query("SELECT cm FROM ChatMessage cm WHERE " +
                     "(cm.senderId = :userId AND cm.receiverId = :adminId) OR " +
                     "(cm.senderId = :adminId AND cm.receiverId = :userId) " +
                     "ORDER BY cm.createdAt ASC")
       List<ChatMessage> findChatHistory(Long userId, Long adminId);

       // Get all conversations for admin (list of users who have chatted)
       @Query("SELECT DISTINCT CASE " +
                     "WHEN cm.senderId = :adminId THEN cm.receiverId " +
                     "ELSE cm.senderId END " +
                     "FROM ChatMessage cm " +
                     "WHERE cm.senderId = :adminId OR cm.receiverId = :adminId")
       List<Long> findAllUserIdsChattedWithAdmin(Long adminId);

       // Get unread message count for user
       @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
                     "WHERE cm.receiverId = :userId AND cm.isRead = false")
       Long countUnreadMessagesForUser(Long userId);

       // Get unread message count for admin from specific user
       @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
                     "WHERE cm.receiverId = :adminId AND cm.senderId = :userId AND cm.isRead = false")
       Long countUnreadMessagesFromUser(Long adminId, Long userId);

       // Mark all messages as read
       @Modifying
       @Query("UPDATE ChatMessage cm SET cm.isRead = true " +
                     "WHERE cm.receiverId = :receiverId AND cm.senderId = :senderId AND cm.isRead = false")
       void markMessagesAsRead(Long receiverId, Long senderId);

       // Get last message between user and admin
       @Query("SELECT cm FROM ChatMessage cm WHERE " +
                     "(cm.senderId = :userId AND cm.receiverId = :adminId) OR " +
                     "(cm.senderId = :adminId AND cm.receiverId = :userId) " +
                     "ORDER BY cm.createdAt DESC LIMIT 1")
       ChatMessage findLastMessage(Long userId, Long adminId);

       /** Balasan bot terakhir ke seorang pelanggan — dipakai membatasi pesan fallback. */
       @Query("SELECT cm FROM ChatMessage cm " +
                     "WHERE cm.receiverId = :userId AND cm.dariBot = true " +
                     "ORDER BY cm.createdAt DESC LIMIT 1")
       ChatMessage findLastBotMessage(Long userId);

       // Get all unread messages for user
       @Query("SELECT cm FROM ChatMessage cm " +
                     "WHERE cm.receiverId = :userId AND cm.isRead = false " +
                     "ORDER BY cm.createdAt ASC")
       List<ChatMessage> findUnreadMessagesForUser(Long userId);
}
