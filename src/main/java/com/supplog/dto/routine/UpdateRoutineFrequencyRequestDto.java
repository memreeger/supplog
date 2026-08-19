package com.supplog.dto.routine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Frequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineFrequencyRequestDto {

    @NotNull(message = "{validation.routine.frequency.required}")
    private Frequency frequency;

    private Set<DayOfWeek> daysOfWeek;

    @Min(value = 1, message = "{validation.routine.dayOfMonth.invalid}")
    @Max(value = 31, message = "{validation.routine.dayOfMonth.invalid}")
    private Integer dayOfMonth;

    @Schema(type = "string", example = "08:30")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime routineTime;
}