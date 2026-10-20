package com.supplog.service.auth.impl;

import com.supplog.entity.PasswordResetToken;
import com.supplog.entity.User;
import com.supplog.enums.RefreshTokenRevokeReason;
import com.supplog.exception.BusinessException;
import com.supplog.exception.PasswordResetTokenInvalidException;
import com.supplog.repository.PasswordResetTokenRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.auth.PasswordResetRequestedEvent;
import com.supplog.service.auth.RefreshTokenService;
import com.supplog.util.TokenHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final String RAW_TOKEN = "raw-reset-token";

    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenService refreshTokenService;
    @Mock ApplicationEventPublisher eventPublisher;

    private PasswordResetServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(
                tokenRepository,
                userRepository,
                passwordEncoder,
                refreshTokenService,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofMinutes(30),
                3
        );

        user = new User();
        user.setId(5L);
        user.setEmail("emre@example.test");
        user.setFirstName("Emre");
        user.setTokenVersion(2);
    }

    @Test
    void unknownEmailIsSilent() {
        when(userRepository.findByEmail("nobody@example.test")).thenReturn(Optional.empty());

        service.requestReset(" Nobody@Example.test ", "1.1.1.1");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void inactiveUserIsSilent() {
        user.setDeleted(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        service.requestReset("emre@example.test", null);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void requestStoresHashedTokenAndPublishesEvent() {
        when(userRepository.findByEmail("emre@example.test")).thenReturn(Optional.of(user));
        when(userRepository.getReferenceById(5L)).thenReturn(user);

        service.requestReset("emre@example.test", "1.1.1.1");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).invalidateOpenTokens(5L, NOW);
        verify(tokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PasswordResetToken saved = tokenCaptor.getValue();
        PasswordResetRequestedEvent event = eventCaptor.getValue();
        assertEquals(TokenHashing.sha256Hex(event.rawToken()), saved.getTokenHash());
        assertNotEquals(event.rawToken(), saved.getTokenHash());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), saved.getExpiresAt());
        assertEquals("1.1.1.1", saved.getRequestIp());
        assertEquals("emre@example.test", event.email());
        assertFalse(event.toString().contains(event.rawToken()));
    }

    @Test
    void hourlyLimitStopsSilently() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(tokenRepository.countByUser_IdAndCreatedAtAfter(5L, NOW.minus(Duration.ofHours(1)))).thenReturn(3L);

        service.requestReset("emre@example.test", null);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void resetChangesPasswordAndEndsAllSessions() {
        PasswordResetToken token = openToken();
        when(tokenRepository.findByTokenHashForUpdate(TokenHashing.sha256Hex(RAW_TOKEN))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("New-pass-123")).thenReturn("encoded");
        user.setMustChangePassword(true);

        service.resetPassword(RAW_TOKEN, "New-pass-123", "New-pass-123");

        assertEquals("encoded", user.getPassword());
        assertEquals(3, user.getTokenVersion());
        assertFalse(user.isMustChangePassword());
        assertEquals(NOW, token.getUsedAt());
        verify(tokenRepository).invalidateOpenTokens(5L, NOW);
        verify(refreshTokenService).revokeAllForUser(5L, RefreshTokenRevokeReason.PASSWORD_RESET);
    }

    @Test
    void mismatchedConfirmationIsRejectedBeforeTokenLookup() {
        assertThrows(BusinessException.class,
                () -> service.resetPassword(RAW_TOKEN, "New-pass-123", "Other-pass-123"));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void unknownUsedOrExpiredTokensAreInvalid() {
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());
        assertThrows(PasswordResetTokenInvalidException.class,
                () -> service.resetPassword(RAW_TOKEN, "New-pass-123", "New-pass-123"));

        PasswordResetToken used = openToken();
        used.setUsedAt(NOW.minusSeconds(5));
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(used));
        assertThrows(PasswordResetTokenInvalidException.class,
                () -> service.resetPassword(RAW_TOKEN, "New-pass-123", "New-pass-123"));

        PasswordResetToken expired = openToken();
        expired.setExpiresAt(NOW);
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(expired));
        assertThrows(PasswordResetTokenInvalidException.class,
                () -> service.resetPassword(RAW_TOKEN, "New-pass-123", "New-pass-123"));

        verifyNoInteractions(passwordEncoder, refreshTokenService);
        verify(tokenRepository, never()).invalidateOpenTokens(anyLong(), any());
    }

    @Test
    void deactivatedUserCannotReset() {
        user.setDeleted(true);
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(openToken()));

        assertThrows(PasswordResetTokenInvalidException.class,
                () -> service.resetPassword(RAW_TOKEN, "New-pass-123", "New-pass-123"));
        verifyNoInteractions(passwordEncoder);
    }

    private PasswordResetToken openToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setUser(user);
        token.setTokenHash(TokenHashing.sha256Hex(RAW_TOKEN));
        token.setCreatedAt(NOW.minusSeconds(60));
        token.setExpiresAt(NOW.plus(Duration.ofMinutes(29)));
        return token;
    }
}
