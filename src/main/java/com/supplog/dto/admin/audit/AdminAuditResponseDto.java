package com.supplog.dto.admin.audit;

import java.time.LocalDateTime;

public record AdminAuditResponseDto(
        Long id,
        Long adminUserId,
        String action,
        String resourceType,
        Long resourceId,
        String oldValue,
        String newValue,
        String reason,
        boolean successful,
        String errorMessage,
        LocalDateTime createdAt
) {
}
