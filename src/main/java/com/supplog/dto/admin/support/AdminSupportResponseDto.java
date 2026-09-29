package com.supplog.dto.admin.support;

import com.supplog.enums.SupportAccessScope;
import com.supplog.enums.SupportStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminSupportResponseDto(
        Long id,
        Long supportedUserId,
        String supportedUsername,
        Long supporterUserId,
        String supporterUsername,
        SupportStatus status,
        SupportAccessScope accessScope,
        Set<Long> permittedRoutineIds,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        LocalDateTime revokedAt
) {
}
