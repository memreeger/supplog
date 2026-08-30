package com.supplog.dto.routine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.DurationType;
import com.supplog.enums.Frequency;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RoutineResponseDto {
    private Long id;
    private Long userId;
    private Long supplementId;
    private String supplementName;
    private Frequency frequency;
    private DurationType durationType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Set<DayOfWeek> daysOfWeek;
    private Integer dayOfMonth;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime routineTime;

    private LocalDate startDate;

    private LocalDate endDate;
}
