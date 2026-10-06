package com.supplog.dto.admin.execution;

import com.supplog.enums.RoutineExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AdminRoutineExecutionResponseDto(
        Long id,
        Long routineId,
        Long userId,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        String scheduledZoneId,
        Instant scheduledAt,
        Instant missedAt,
        RoutineExecutionStatus status,
        Instant resolvedAt
) {
}
