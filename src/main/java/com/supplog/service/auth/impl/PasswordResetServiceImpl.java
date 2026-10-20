package com.supplog.service.auth.impl;

import com.supplog.entity.PasswordResetToken;
import com.supplog.entity.User;
import com.supplog.enums.RefreshTokenRevokeReason;
import com.supplog.exception.BusinessException;
import com.supplog.exception.PasswordResetTokenInvalidException;
import com.supplog.repository.PasswordResetTokenRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.auth.PasswordResetRequestedEvent;
import com.supplog.service.auth.PasswordResetService;
import com.supplog.service.auth.RefreshTokenService;
import com.supplog.util.InputNormalizer;
import com.supplog.util.TokenHashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final Duration CLEANUP_RETENTION = Duration.ofDays(1);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration tokenTtl;
    private final int maxRequestsPerHour;

    @Autowired
    public PasswordResetServiceImpl(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes,
            @Value("${app.password-reset.max-requests-per-hour:3}") int maxRequestsPerHour
    ) {
        this(
                passwordResetTokenRepository,
                userRepository,
                passwordEncoder,
                refreshTokenService,
                eventPublisher,
                Clock.systemUTC(),
                Duration.ofMinutes(tokenTtlMinutes),
                maxRequestsPerHour
        );
    }

    PasswordResetServiceImpl(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            Duration tokenTtl,
            int maxRequestsPerHour
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
        this.maxRequestsPerHour = maxRequestsPerHour;
    }

    @Override
    @Transactional
    public void requestReset(String email, String requestIp) {
        Instant now = clock.instant();

        Optional<User> candidate = userRepository
                .findByEmail(InputNormalizer.normalizeEmail(email))
                .filter(user -> !user.isDeleted());

        if (candidate.isEmpty()) {
            log.info("Password reset requested for unknown or inactive e-mail");
            return;
        }

        User user = candidate.get();
        long recentRequests = passwordResetTokenRepository
                .countByUser_IdAndCreatedAtAfter(user.getId(), now.minus(RATE_WINDOW));

        if (recentRequests >= maxRequestsPerHour) {
            log.warn("Password reset rate limit reached, userId={}", user.getId());
            return;
        }

        // invalidateOpenTokens persistence context'i temizler; ihtiyaç duyulan alanlar önce okunur.
        String recipient = user.getEmail();
        String firstName = user.getFirstName();

        passwordResetTokenRepository.invalidateOpenTokens(user.getId(), now);

        String rawToken = TokenHashing.generateToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(userRepository.getReferenceById(user.getId()));
        token.setTokenHash(TokenHashing.sha256Hex(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(tokenTtl));
        token.setRequestIp(requestIp);
        passwordResetTokenRepository.save(token);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                recipient,
                firstName,
                rawToken,
                LocaleContextHolder.getLocale()
        ));
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("user.password.not.match");
        }

        if (rawToken == null || rawToken.isBlank()) {
            throw new PasswordResetTokenInvalidException();
        }

        Instant now = clock.instant();

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashForUpdate(TokenHashing.sha256Hex(rawToken))
                .orElseThrow(PasswordResetTokenInvalidException::new);

        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new PasswordResetTokenInvalidException();
        }

        User user = token.getUser();

        if (user.isDeleted()) {
            throw new PasswordResetTokenInvalidException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        // Tüm access ve refresh token'lar geçersiz olur.
        user.setTokenVersion(user.getTokenVersion() + 1);
        token.setUsedAt(now);

        Long userId = user.getId();
        passwordResetTokenRepository.invalidateOpenTokens(userId, now);
        refreshTokenService.revokeAllForUser(userId, RefreshTokenRevokeReason.PASSWORD_RESET);

        log.info("Password reset completed, userId={}", userId);
    }

    @Override
    @Scheduled(cron = "${app.password-reset.cleanup-cron:0 45 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = passwordResetTokenRepository.deleteExpiredOrUsedBefore(
                clock.instant().minus(CLEANUP_RETENTION)
        );
        log.info("Password reset token cleanup finished, deleted={}", deleted);
    }
}
