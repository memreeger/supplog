package com.supplog.service.auth.impl;

import com.supplog.entity.RefreshToken;
import com.supplog.entity.User;
import com.supplog.enums.ClientPlatform;
import com.supplog.enums.RefreshTokenRevokeReason;
import com.supplog.exception.AuthTokenException;
import com.supplog.repository.RefreshTokenRepository;
import com.supplog.service.auth.ClientInfo;
import com.supplog.service.auth.IssuedRefreshToken;
import com.supplog.util.TokenHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final String RAW_TOKEN = "raw-refresh-token";

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(
                refreshTokenRepository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofDays(30),
                Duration.ofDays(90),
                Duration.ofSeconds(10)
        );

        user = new User();
        user.setId(7L);
        user.setTokenVersion(3);
    }

    @Test
    void issueStoresOnlyHashAndStartsNewFamily() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken issued = service.issue(
                user,
                new ClientInfo("Pixel", ClientPlatform.ANDROID, "1.2.3.4", "agent")
        );

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertNotEquals(issued.rawToken(), saved.getTokenHash());
        assertEquals(TokenHashing.sha256Hex(issued.rawToken()), saved.getTokenHash());
        assertEquals(3, saved.getTokenVersion());
        assertEquals(NOW.plus(Duration.ofDays(30)), saved.getExpiresAt());
        assertEquals(NOW.plus(Duration.ofDays(90)), saved.getFamilyExpiresAt());
        assertEquals(ClientPlatform.ANDROID, saved.getPlatform());
        assertNotNull(saved.getFamilyId());
    }

    @Test
    void rotateIssuesNewTokenInSameFamilyAndMarksOldAsRotated() {
        RefreshToken current = activeToken();
        when(refreshTokenRepository.findByTokenHashForUpdate(TokenHashing.sha256Hex(RAW_TOKEN)))
                .thenReturn(Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken next = service.rotate(RAW_TOKEN, null);

        assertNotEquals(RAW_TOKEN, next.rawToken());
        assertEquals(NOW, current.getRotatedAt());
        assertNotNull(current.getReplacedBy());
        assertEquals(current.getFamilyId(), current.getReplacedBy().getFamilyId());
        assertEquals("Pixel", current.getReplacedBy().getDeviceName());
    }

    @Test
    void rotateNeverExtendsBeyondFamilyAbsoluteExpiry() {
        RefreshToken current = activeToken();
        current.setFamilyExpiresAt(NOW.plus(Duration.ofDays(2)));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken next = service.rotate(RAW_TOKEN, null);

        assertEquals(NOW.plus(Duration.ofDays(2)), next.expiresAt());
    }

    @Test
    void unknownTokenIsInvalid() {
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertCode("REFRESH_TOKEN_INVALID", () -> service.rotate(RAW_TOKEN, null));
    }

    @Test
    void blankTokenIsInvalidWithoutQuery() {
        assertCode("REFRESH_TOKEN_INVALID", () -> service.rotate(" ", null));
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokedTokenIsInvalid() {
        RefreshToken current = activeToken();
        current.setRevokedAt(NOW.minusSeconds(60));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        assertCode("REFRESH_TOKEN_INVALID", () -> service.rotate(RAW_TOKEN, null));
        verify(refreshTokenRepository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void reuseWithinGraceIsRejectedWithoutRevokingFamily() {
        RefreshToken current = activeToken();
        current.setRotatedAt(NOW.minusSeconds(5));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        assertCode("REFRESH_TOKEN_INVALID", () -> service.rotate(RAW_TOKEN, null));
        verify(refreshTokenRepository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void reuseAfterGraceRevokesWholeFamily() {
        RefreshToken current = activeToken();
        current.setRotatedAt(NOW.minusSeconds(60));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        assertCode("REFRESH_TOKEN_REUSED", () -> service.rotate(RAW_TOKEN, null));
        verify(refreshTokenRepository).revokeFamily(
                current.getFamilyId(), RefreshTokenRevokeReason.REUSE_DETECTED, NOW
        );
    }

    @Test
    void expiredTokenIsRejected() {
        RefreshToken current = activeToken();
        current.setExpiresAt(NOW);
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        assertCode("REFRESH_TOKEN_EXPIRED", () -> service.rotate(RAW_TOKEN, null));
    }

    @Test
    void inactiveUserRevokesFamily() {
        RefreshToken current = activeToken();
        user.setDeleted(true);
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        assertCode("REFRESH_TOKEN_INVALID", () -> service.rotate(RAW_TOKEN, null));
        verify(refreshTokenRepository).revokeFamily(
                current.getFamilyId(), RefreshTokenRevokeReason.USER_INACTIVE, NOW
        );
    }

    @Test
    void changedTokenVersionRevokesFamily() {
        RefreshToken current = activeToken();
        user.setTokenVersion(4);
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(current));

        assertCode("TOKEN_REVOKED", () -> service.rotate(RAW_TOKEN, null));
        verify(refreshTokenRepository).revokeFamily(
                current.getFamilyId(), RefreshTokenRevokeReason.TOKEN_VERSION_CHANGED, NOW
        );
    }

    @Test
    void revokeIsIdempotentForUnknownOrBlankToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revoke(null);
        service.revoke("");
        service.revoke(RAW_TOKEN);

        verify(refreshTokenRepository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void revokeRevokesWholeFamily() {
        RefreshToken current = activeToken();
        when(refreshTokenRepository.findByTokenHash(TokenHashing.sha256Hex(RAW_TOKEN)))
                .thenReturn(Optional.of(current));

        service.revoke(RAW_TOKEN);

        verify(refreshTokenRepository).revokeFamily(
                eq(current.getFamilyId()), eq(RefreshTokenRevokeReason.LOGOUT), eq(NOW)
        );
    }

    private RefreshToken activeToken() {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setUser(user);
        token.setTokenHash(TokenHashing.sha256Hex(RAW_TOKEN));
        token.setFamilyId(UUID.randomUUID());
        token.setTokenVersion(3);
        token.setCreatedAt(NOW.minus(Duration.ofDays(1)));
        token.setExpiresAt(NOW.plus(Duration.ofDays(29)));
        token.setFamilyExpiresAt(NOW.plus(Duration.ofDays(89)));
        token.setDeviceName("Pixel");
        token.setPlatform(ClientPlatform.ANDROID);
        return token;
    }

    private static void assertCode(String expectedCode, Runnable action) {
        AuthTokenException exception = assertThrows(AuthTokenException.class, action::run);
        assertEquals(expectedCode, exception.getCode());
    }
}
