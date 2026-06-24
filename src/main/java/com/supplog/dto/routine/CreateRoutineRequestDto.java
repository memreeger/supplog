package com.supplog.dto.routine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supplog.enums.DayOfWeek;
import com.supplog.enums.Period;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateRoutineRequestDto extends BaseRoutineDto {
    @NotNull(message = "{validation.routine.day.required}")
    private DayOfWeek dayName;

    @Schema(type = "string", example = "08:30") // swaggerda local date girişi düzenlemek için
    @NotNull(message = "{validation.routine.time.required}")
    private LocalTime routineTime;

    @NotNull(message = "{validation.routine.period.required}")
    private Period period;
}
