package com.supplog.dto.admin.routine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Period;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRoutineRequestDtoAdmin {

    @NotNull(message = "{validation.routine.day.required}")
    private DayOfWeek dayName;

    @NotNull(message = "{validation.routine.time.required}")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime routineTime;

    @NotNull(message = "{validation.routine.period.required}")
    private Period period;
}
