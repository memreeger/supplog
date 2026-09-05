package com.supplog.dto.routineExecution;

import com.supplog.enums.RoutineExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class RoutineExecutionResponseDto {

    private Long id;
    private Long routineId;

    private LocalDate scheduledDate;
    private LocalTime scheduledTime;

    private String scheduledZoneId;
    private Instant scheduledAt;

    private RoutineExecutionStatus status;
    private Instant resolvedAt;
}