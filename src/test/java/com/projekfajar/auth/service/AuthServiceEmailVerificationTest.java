package com.projekfajar.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.projekfajar.auth.model.EmailVerification;
import com.projekfajar.auth.repository.EmailVerificationRepository;
import com.projekfajar.auth.repository.LoginOtpRepository;
import com.projekfajar.exception.InvalidOtpException;
import com.projekfajar.exception.OtpExpiredException;
import com.projekfajar.notification.service.NotificationService;
import com.projekfajar.user.model.User;
import com.projekfajar.user.repository.UserRepository;
import com.projekfajar.util.JwtUtil;
import com.projekfajar.util.OtpGenerator;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Menguji verifikasi kepemilikan email: kode benar, salah, kedaluwarsa,
 * dan pembatasan jumlah percobaan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceEmailVerificationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpGenerator otpGenerator;
    @Mock
    private LoginOtpRepository loginOtpRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailVerificationRepository emailVerificationRepository;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("pembeli@test.com")
                .namaLengkap("Pembeli")
                .emailTerverifikasi(false)
                .build();

        when(userRepository.findByEmail("pembeli@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private EmailVerification kodeAktif(String kode, LocalDateTime expiredAt) {
        return EmailVerification.builder()
                .id(1L)
                .user(user)
                .kode(kode)
                .percobaan(0)
                .used(false)
                .expiredAt(expiredAt)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Kode benar menandai email terverifikasi dan menutup kodenya")
    void kodeBenarMemverifikasi() {
        EmailVerification kode = kodeAktif("123456", LocalDateTime.now().plusMinutes(10));
        when(emailVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(kode));

        authService.verifikasiEmail("pembeli@test.com", "123456");

        assertThat(user.getEmailTerverifikasi()).isTrue();
        assertThat(user.getEmailTerverifikasiAt()).isNotNull();
        assertThat(kode.getUsed()).isTrue();
    }

    @Test
    @DisplayName("Kode salah ditolak, percobaan bertambah, email tetap belum terverifikasi")
    void kodeSalahDitolak() {
        EmailVerification kode = kodeAktif("123456", LocalDateTime.now().plusMinutes(10));
        when(emailVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(kode));

        assertThatThrownBy(() -> authService.verifikasiEmail("pembeli@test.com", "999999"))
                .isInstanceOf(InvalidOtpException.class);

        assertThat(kode.getPercobaan()).isEqualTo(1);
        assertThat(kode.getUsed()).isFalse();
        assertThat(user.getEmailTerverifikasi()).isFalse();
    }

    @Test
    @DisplayName("Kode dibatalkan setelah 5 kali salah")
    void kodeDibatalkanSetelahBatasPercobaan() {
        EmailVerification kode = kodeAktif("123456", LocalDateTime.now().plusMinutes(10));
        kode.setPercobaan(4); // percobaan salah berikutnya menyentuh batas
        when(emailVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(kode));

        assertThatThrownBy(() -> authService.verifikasiEmail("pembeli@test.com", "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("dibatalkan");

        assertThat(kode.getUsed()).isTrue();
    }

    @Test
    @DisplayName("Kode kedaluwarsa ditolak")
    void kodeKedaluwarsaDitolak() {
        EmailVerification kode = kodeAktif("123456", LocalDateTime.now().minusMinutes(1));
        when(emailVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(kode));

        assertThatThrownBy(() -> authService.verifikasiEmail("pembeli@test.com", "123456"))
                .isInstanceOf(OtpExpiredException.class);

        assertThat(user.getEmailTerverifikasi()).isFalse();
    }

    @Test
    @DisplayName("Email yang sudah terverifikasi tidak diproses ulang")
    void emailSudahTerverifikasi() {
        user.setEmailTerverifikasi(true);

        authService.verifikasiEmail("pembeli@test.com", "123456");

        verify(emailVerificationRepository, never())
                .findTopByUserAndUsedFalseOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Kirim ulang menerbitkan kode baru dan membatalkan kode lama")
    void kirimUlangMembatalkanKodeLama() {
        EmailVerification lama = kodeAktif("111111", LocalDateTime.now().plusMinutes(5));
        when(emailVerificationRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(lama));
        when(otpGenerator.generate6Digit()).thenReturn("222222");

        authService.kirimUlangVerifikasi("pembeli@test.com");

        assertThat(lama.getUsed()).isTrue();
        verify(emailService).sendEmailVerification(anyString(), anyString(), anyString());
    }
}
