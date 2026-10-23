package com.supplog.service.notification;

import com.supplog.enums.NotificationType;

import java.util.Map;

/**
 * Inbox satırı yazıldı/güncellendi. İşlem commit edildikten sonra push "kapı zili" olarak gönderilir;
 * push başarısız olsa da inbox satırı yerinde kalır.
 */
public record NotificationCreatedEvent(
        Long notificationId,
        Long recipientUserId,
        NotificationType type,
        String dedupeKey,
        Long routineId,
        Map<String, Object> params
) {
}
