package com.supplog.service.auth.impl;

import com.supplog.entity.RefreshToken;
import com.supplog.entity.User;
import com.supplog.enums.ClientPlatform;
import com.supplog.enums.RefreshTokenRevokeReason;
import com.supplog.exception.AuthTokenException;
import com.supplog.repository.RefreshTokenRepository;
import com.supplog.service.auth.ClientInfo;
import com.supplog.service.auth.IssuedRefreshToken;
import com.supplog.service.auth.RefreshTokenService;
import com.supplog.util.TokenHashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REUSED = "REFRESH_TOKEN_REUSED";
    public static final String TOKEN_REVOKED = "TOKEN_REVOKED";

    private static final int MAX_DEVICE_NAME_LENGTH = 100;
    private static final int MAX_IP_ADDRESS_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final Duration CLEANUP_RETENTION = Duration.ofDays(7);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final Duration slidingLifetime;
    private final Duration absoluteLifetime;
    private final Duration reuseGrace;

    @Autowired
    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-expiration-days:30}") long refreshExpirationDays,
            @Value("${app.jwt.refresh-absolute-expiration-days:90}") long absoluteExpirationDays,
            @Value("${app.jwt.refresh-reuse-grace-seconds:10}") long reuseGraceSeconds
    ) {
        this(
                refreshTokenRepository,
                Clock.systemUTC(),
                Duration.ofDays(refreshExpirationDays),
                Duration.ofDays(absoluteExpirationDays),
                Duration.ofSeconds(reuseGraceSeconds)
        );
    }

    RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            Clock clock,
            Duration slidingLifetime,
            Duration absoluteLifetime,
            Duration reuseGrace
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
        this.slidingLifetime = slidingLifetime;
        this.absoluteLifetime = absoluteLifetime;
        this.reuseGrace = reuseGrace;
    }

    @Override
    @Transactional
    public IssuedRefreshToken issue(User user, ClientInfo clientInfo) {
        Instant now = clock.instant();

        CreatedToken token = createToken(
                user,
                UUID.randomUUID(),
                now.plus(absoluteLifetime),
                clientInfo,
                now
        );

        return new IssuedRefreshToken(user, token.rawToken, token.entity.getExpiresAt());
    }

    // Hata durumunda yapılan aile iptali kalıcı olmalı, bu yüzden rollback yok.
    @Override
    @Transactional(noRollbackFor = AuthTokenException.class)
    public IssuedRefreshToken rotate(String rawToken, ClientInfo clientInfo) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalid();
        }

        Instant now = clock.instant();

        RefreshToken current = refreshTokenRepository
                .findByTokenHashForUpdate(TokenHashing.sha256Hex(rawToken))
                .orElseThrow(RefreshTokenServiceImpl::invalid);

        if (current.getRevokedAt() != null) {
            throw invalid();
        }

        if (current.getRotatedAt() != null) {
            // Paralel isteklerde aynı token kısa süre içinde iki kez gelebilir.
            if (!current.getRotatedAt().plus(reuseGrace).isBefore(now)) {
                throw invalid();
            }

            log.warn("Refresh token reuse detected, familyId={}", current.getFamilyId());
            revokeFamily(current, RefreshTokenRevokeReason.REUSE_DETECTED, now);
            throw new AuthTokenException(REFRESH_TOKEN_REUSED, "auth.refresh.reused");
        }

        if (!current.getExpiresAt().isAfter(now)) {
            throw new AuthTokenException(REFRESH_TOKEN_EXPIRED, "auth.refresh.expired");
        }

        User user = current.getUser();

        if (user.isDeleted()) {
            revokeFamily(current, RefreshTokenRevokeReason.USER_INACTIVE, now);
            throw invalid();
        }

        // Şifre değişimi, admin işlemleri vb. tokenVersion'ı artırır.
        if (user.getTokenVersion() != current.getTokenVersion()) {
            revokeFamily(current, RefreshTokenRevokeReason.TOKEN_VERSION_CHANGED, now);
            throw new AuthTokenException(TOKEN_REVOKED, "auth.token.revoked");
        }

        CreatedToken next = createToken(
                user,
                current.getFamilyId(),
                current.getFamilyExpiresAt(),
                withFallbackDevice(clientInfo, current),
                now
        );

        current.setRotatedAt(now);
        current.setLastUsedAt(now);
        current.setReplacedBy(next.entity);

        return new IssuedRefreshToken(user, next.rawToken, next.entity.getExpiresAt());
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHash(TokenHashing.sha256Hex(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> revokeFamily(
                        token,
                        RefreshTokenRevokeReason.LOGOUT,
                        clock.instant()
                ));
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId, RefreshTokenRevokeReason reason) {
        refreshTokenRepository.revokeAllByUserId(userId, reason, clock.instant());
    }

    @Override
    @Scheduled(cron = "${app.jwt.refresh-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredOrRevokedBefore(
                clock.instant().minus(CLEANUP_RETENTION)
        );
        log.info("Refresh token cleanup finished, deleted={}", deleted);
    }

    private void revokeFamily(
            RefreshToken token,
            RefreshTokenRevokeReason reason,
            Instant now
    ) {
        refreshTokenRepository.revokeFamily(token.getFamilyId(), reason, now);
    }

    private CreatedToken createToken(
            User user,
            UUID familyId,
            Instant familyExpiresAt,
            ClientInfo clientInfo,
            Instant now
    ) {
        String rawToken = TokenHashing.generateToken();
        Instant slidingExpiry = now.plus(slidingLifetime);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHashing.sha256Hex(rawToken));
        token.setFamilyId(familyId);
        token.setTokenVersion(user.getTokenVersion());
        token.setFamilyExpiresAt(familyExpiresAt);
        token.setExpiresAt(
                slidingExpiry.isBefore(familyExpiresAt) ? slidingExpiry : familyExpiresAt
        );
        token.setCreatedAt(now);
        applyClientInfo(token, clientInfo);

        return new CreatedToken(refreshTokenRepository.save(token), rawToken);
    }

    private static void applyClientInfo(
            RefreshToken token,
            ClientInfo clientInfo
    ) {
        if (clientInfo == null) {
            return;
        }

        token.setDeviceName(truncate(clientInfo.deviceName(), MAX_DEVICE_NAME_LENGTH));
        token.setPlatform(
                clientInfo.platform() != null ? clientInfo.platform() : ClientPlatform.UNKNOWN
        );
        token.setIpAddress(truncate(clientInfo.ipAddress(), MAX_IP_ADDRESS_LENGTH));
        token.setUserAgent(truncate(clientInfo.userAgent(), MAX_USER_AGENT_LENGTH));
    }

    // Refresh isteğinde cihaz bilgisi gelmezse login'deki bilgi korunur.
    private static ClientInfo withFallbackDevice(
            ClientInfo clientInfo,
            RefreshToken current
    ) {
        if (clientInfo == null) {
            return new ClientInfo(current.getDeviceName(), current.getPlatform(), null, null);
        }

        boolean hasPlatform = clientInfo.platform() != null
                && clientInfo.platform() != ClientPlatform.UNKNOWN;

        return new ClientInfo(
                clientInfo.deviceName() != null ? clientInfo.deviceName() : current.getDeviceName(),
                hasPlatform ? clientInfo.platform() : current.getPlatform(),
                clientInfo.ipAddress(),
                clientInfo.userAgent()
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static AuthTokenException invalid() {
        return new AuthTokenException(REFRESH_TOKEN_INVALID, "auth.refresh.invalid");
    }

    // Kaydedilen entity ile istemciye dönecek düz token birlikte taşınır.
    private record CreatedToken(RefreshToken entity, String rawToken) {
    }
}
