package com.supplog.service.notification;

import com.supplog.dto.notification.NotificationPreferenceDto;

public interface NotificationPreferenceService {

    // Satır yoksa varsayılanlar (hepsi açık, kilit ekranı detayı gizli değil) döner.
    NotificationPreferenceDto get(Long userId);

    NotificationPreferenceDto update(Long userId, NotificationPreferenceDto request);
}
