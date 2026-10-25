package com.supplog.service.notification.impl;

import com.supplog.dto.notification.DeviceRegisterDto;
import com.supplog.entity.DeviceToken;
import com.supplog.repository.DeviceTokenRepository;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.user.ActiveUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class DeviceTokenServiceImpl implements DeviceTokenService {

    // Kullanıcı başına saklanan en çok cihaz; fazlası en eski görülenden başlayarak silinir.
    static final int MAX_DEVICES_PER_USER = 20;

    private final DeviceTokenRepository deviceTokenRepository;
    private final ActiveUserService activeUserService;
    private final Clock clock;
    private final Duration staleAfter;

    @Autowired
    public DeviceTokenServiceImpl(
            DeviceTokenRepository deviceTokenRepository,
            ActiveUserService activeUserService,
            @Value("${app.notifications.device-token-stale-days:60}") long staleDays
    ) {
        this(deviceTokenRepository, activeUserService, Clock.systemUTC(), Duration.ofDays(staleDays));
    }

    // Testlerde saat enjekte edilebilsin diye.
    DeviceTokenServiceImpl(
            DeviceTokenRepository deviceTokenRepository,
            ActiveUserService activeUserService,
            Clock clock,
            Duration staleAfter
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.activeUserService = activeUserService;
        this.clock = clock;
        this.staleAfter = staleAfter;
    }

    @Override
    @Transactional
    public void register(Long userId, DeviceRegisterDto request) {
        activeUserService.requireActiveUser(userId);

        deviceTokenRepository.upsert(
                userId,
                request.getPushToken().trim(),
                request.getPlatform().name(),
                normalizeLocale(request.getLocale()),
                request.getAppVersion(),
                clock.instant()
        );
        deviceTokenRepository.trimToNewest(userId, MAX_DEVICES_PER_USER);
    }

    @Override
    @Transactional
    public void unregister(Long userId, String pushToken) {
        deviceTokenRepository.deleteByPushTokenAndUserId(pushToken, userId);
    }

    @Override
    @Transactional
    public void deleteAllForUser(Long userId) {
        deviceTokenRepository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceToken> tokensFor(Long userId) {
        return deviceTokenRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public void removeInvalid(String pushToken) {
        deviceTokenRepository.deleteByPushToken(pushToken);
    }

    @Override
    @Scheduled(cron = "${app.notifications.device-token-cleanup-cron:0 45 4 * * *}")
    @Transactional
    public void deleteStale() {
        int deleted = deviceTokenRepository.deleteByLastSeenAtBefore(clock.instant().minus(staleAfter));
        log.info("Device token cleanup finished, deleted={}", deleted);
    }

    // "tr_TR" → "tr-TR"; boş/null değer null kalır (gönderimde varsayılan dil kullanılır).
    private String normalizeLocale(String locale) {
        return locale == null || locale.isBlank() ? null : locale.trim().replace('_', '-');
    }
}
