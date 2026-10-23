package com.supplog.service.notification.push;

import com.supplog.config.AsyncConfig;
import com.supplog.entity.DeviceToken;
import com.supplog.entity.NotificationPreference;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.notification.NotificationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Locale;

/**
 * Inbox satırı commit edildikten sonra, ayrı bir thread'de kullanıcının cihazlarına push gönderir.
 * Hata verirse yalnızca loglanır: bildirim zaten çanda duruyor, push yalnızca dikkat çekmek için.
 */
@Slf4j
@Component
public class NotificationPushListener {

    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("tr");

    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenService deviceTokenService;
    private final PushMessageFactory messageFactory;
    private final PushSender pushSender;

    public NotificationPushListener(
            NotificationPreferenceRepository preferenceRepository,
            DeviceTokenService deviceTokenService,
            PushMessageFactory messageFactory,
            PushSender pushSender
    ) {
        this.preferenceRepository = preferenceRepository;
        this.deviceTokenService = deviceTokenService;
        this.messageFactory = messageFactory;
        this.pushSender = pushSender;
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationCreatedEvent event) {
        try {
            push(event);
        } catch (Exception e) {
            log.error("Push failed for notificationId={}", event.notificationId(), e);
        }
    }

    void push(NotificationCreatedEvent event) {
        NotificationPreference preference = preferenceRepository.findById(event.recipientUserId()).orElse(null);
        if (preference != null && !preference.isPushEnabled()) {
            return;
        }
        boolean hideDetails = preference != null && preference.isHideDetailsInPush();

        List<DeviceToken> devices = deviceTokenService.tokensFor(event.recipientUserId());
        int sent = 0;
        int failed = 0;
        int removed = 0;
        for (DeviceToken device : devices) {
            PushMessageFactory.Content content = messageFactory.create(
                    event.type(), event.params(), event.notificationId(), event.routineId(),
                    event.dedupeKey(), localeOf(device), hideDetails);

            PushOutcome outcome = pushSender.send(device.getPushToken(), new PushPayload(
                    content.title(), content.body(), content.data(), content.tag(), content.channelId()));

            switch (outcome) {
                case SENT -> sent++;
                case INVALID_TOKEN -> {
                    deviceTokenService.removeInvalid(device.getPushToken());
                    removed++;
                }
                case FAILED -> failed++;
            }
        }
        if (!devices.isEmpty()) {
            log.info("Push finished notificationId={} devices={} sent={} failed={} removed={}",
                    event.notificationId(), devices.size(), sent, failed, removed);
        }
    }

    private static Locale localeOf(DeviceToken device) {
        return device.getLocale() == null ? DEFAULT_LOCALE : Locale.forLanguageTag(device.getLocale());
    }
}
