package com.supplog.dto.routine;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineDayOfMonthRequestDto {

    @NotNull(message = "{routine.day.of.month.required}")
    @Min(value = 1, message = "{validation.routine.dayOfMonth.invalid}")
    @Max(value = 31, message = "{validation.routine.dayOfMonth.invalid}")
    private Integer dayOfMonth;
}