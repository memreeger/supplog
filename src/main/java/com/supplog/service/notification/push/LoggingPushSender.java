package com.supplog.service.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Varsayılan gönderici (PUSH_PROVIDER=log): gerçek push atmaz, yalnızca bir satır loglar.
 * Token ve bildirim metni loglanmaz; yerelde ve testte güvenle kullanılır.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.push-provider", havingValue = "log", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    @Override
    public PushOutcome send(String pushToken, PushPayload payload) {
        log.info("Push (log provider) channel={} type={}", payload.channelId(), payload.data().get("type"));
        return PushOutcome.SENT;
    }
}
