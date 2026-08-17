package com.projekfajar.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.projekfajar.auth.model.Role;
import com.projekfajar.chat.dto.ChatMessageRequest;
import com.projekfajar.chat.model.ChatMessage;
import com.projekfajar.chat.repository.ChatMessageRepository;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.settings.service.SettingService;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;

/**
 * Menguji kapan bot boleh ikut menjawab: hanya saat admin sedang offline,
 * dan tidak pernah membalas pesan admin sendiri.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private OnlineUserTracker onlineUserTracker;
    @Mock
    private ChatBotService chatBotService;
    @Mock
    private SettingService settingService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ChatService chatService;

    private User pelanggan;
    private User admin;

    @BeforeEach
    void setUp() {
        pelanggan = User.builder().id(1L).namaLengkap("Budi").email("budi@test.com")
                .role(Role.USER).build();
        admin = User.builder().id(2L).namaLengkap("Admin").email("admin@test.com")
                .role(Role.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(pelanggan));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(settingService.getValue("chatbot.enabled")).thenReturn("true");
    }

    private ChatMessageRequest pesanKeAdmin() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setReceiverId(2L);
        request.setMessage("ongkir ke Jawa Timur berapa?");
        return request;
    }

    @Test
    @DisplayName("Admin online: bot tidak ikut campur")
    void adminOnlineBotDiam() {
        when(onlineUserTracker.isUserOnline(2L)).thenReturn(true);

        chatService.sendMessage(1L, pesanKeAdmin());

        // Hanya pesan pelanggan yang tersimpan
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatBotService, never()).susunBalasan(any(), anyString());
    }

    @Test
    @DisplayName("Admin offline: bot menjawab dan balasannya ditandai dariBot")
    void adminOfflineBotMenjawab() {
        when(onlineUserTracker.isUserOnline(2L)).thenReturn(false);
        when(chatBotService.susunBalasan(any(), anyString()))
                .thenReturn(Optional.of("Ongkir ke Jawa Timur sebesar Rp 25.000."));

        chatService.sendMessage(1L, pesanKeAdmin());

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(captor.capture());

        ChatMessage balasanBot = captor.getAllValues().get(1);
        assertThat(balasanBot.getDariBot()).isTrue();
        assertThat(balasanBot.getSenderId()).isEqualTo(2L);   // atas nama admin
        assertThat(balasanBot.getReceiverId()).isEqualTo(1L); // ke pelanggan
        assertThat(balasanBot.getSenderRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Chatbot dimatikan lewat pengaturan: tidak ada balasan otomatis")
    void chatbotDimatikan() {
        when(settingService.getValue("chatbot.enabled")).thenReturn("false");
        when(onlineUserTracker.isUserOnline(2L)).thenReturn(false);

        chatService.sendMessage(1L, pesanKeAdmin());

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatBotService, never()).susunBalasan(any(), anyString());
    }

    @Test
    @DisplayName("Bot tidak tahu jawabannya: kirim fallback dan beri tahu admin")
    void fallbackMemberiTahuAdmin() {
        when(onlineUserTracker.isUserOnline(2L)).thenReturn(false);
        when(chatBotService.susunBalasan(any(), anyString())).thenReturn(Optional.empty());
        when(chatMessageRepository.findLastBotMessage(1L)).thenReturn(null);
        when(settingService.getValue("chatbot.pesan_fallback")).thenReturn("Ditunggu ya.");

        chatService.sendMessage(1L, pesanKeAdmin());

        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
        verify(notificationService).sendPertanyaanBelumTerjawab(any(), anyString());
    }

    @Test
    @DisplayName("Pesan dari admin tidak pernah memicu bot (tidak ada loop)")
    void pesanAdminTidakMemicuBot() {
        ChatMessageRequest keUser = new ChatMessageRequest();
        keUser.setReceiverId(1L);
        keUser.setMessage("Baik kak, saya cek dulu.");

        chatService.sendMessage(2L, keUser);

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(chatBotService, never()).susunBalasan(any(), anyString());
    }
}
