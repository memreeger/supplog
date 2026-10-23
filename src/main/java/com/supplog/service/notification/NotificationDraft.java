package com.supplog.service.notification;

import com.supplog.enums.NotificationType;

import java.util.Map;

/**
 * Inbox'a yazılacak bildirimin içeriği. {@code dedupeKey} aynı olayın tek satırda kalmasını sağlar
 * (örn. "exec:123:REMINDER": her dilim aynı satırı günceller).
 */
public record NotificationDraft(
        Long recipientUserId,
        NotificationType type,
        String dedupeKey,
        Long executionId,
        Long routineId,
        Long actorUserId,
        Map<String, Object> params
) {
}
