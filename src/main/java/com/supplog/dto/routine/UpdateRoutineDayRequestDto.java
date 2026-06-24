package com.supplog.dto.routine;

import com.supplog.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineDayRequestDto {
    @NotNull(message = "{validation.routine.day.required}")
    private DayOfWeek dayName;
}
