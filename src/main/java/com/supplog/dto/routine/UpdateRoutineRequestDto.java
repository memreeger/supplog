package com.supplog.dto.routine;

import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Period;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineRequestDto {
    private DayOfWeek dayName;
    private LocalTime routineTime;
    private Period period;
}
