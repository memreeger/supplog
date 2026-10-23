package com.supplog.service.notification;

import com.supplog.dto.notification.ReminderSettingsDto;

public interface ReminderSettingsService {

    // Satır yoksa varsayılan (açık, 30 dk önce) döner. Sadece rutinin sahibi erişir.
    ReminderSettingsDto get(Long userId, Long routineId);

    ReminderSettingsDto update(Long userId, Long routineId, ReminderSettingsDto request);
}
