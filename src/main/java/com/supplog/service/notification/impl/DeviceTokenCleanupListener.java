package com.supplog.service.notification.impl;

import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.notification.UserSessionsRevokedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Senkron dinler: olayı yayınlayan işlemle aynı transaction'da çalışır, o geri alınırsa bu da geri alınır.
@Component
@RequiredArgsConstructor
public class DeviceTokenCleanupListener {

    private final DeviceTokenService deviceTokenService;

    @EventListener
    public void onSessionsRevoked(UserSessionsRevokedEvent event) {
        deviceTokenService.deleteAllForUser(event.userId());
    }
}
