package com.supplog.dto.support;

import com.supplog.enums.DayOfWeek;
import com.supplog.enums.DurationType;
import com.supplog.enums.Frequency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@AllArgsConstructor
public class SupportRoutineResponseDto {

    private Long routineId;

    private String supplementName;
    private String supplementDosage;

    private Frequency frequency;
    private Set<DayOfWeek> daysOfWeek;
    private Integer dayOfMonth;
    private LocalTime routineTime;

    private DurationType durationType;
    private LocalDate startDate;
    private LocalDate endDate;
}