package com.supplog.dto.notification;

import com.supplog.entity.Notification;
import com.supplog.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;

    private NotificationType type;

    // Metin değil veri: istemci type + params ile kendi dilinde çizer.
    private Map<String, Object> params;

    private Long executionId;

    private Long routineId;

    private Long actorUserId;

    private Long nudgeId;

    private boolean read;

    private Instant createdAt;

    public static NotificationResponseDto from(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getType(),
                n.getParams(),
                n.getExecutionId(),
                n.getRoutineId(),
                n.getActorUserId(),
                n.getNudgeId(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }
}
