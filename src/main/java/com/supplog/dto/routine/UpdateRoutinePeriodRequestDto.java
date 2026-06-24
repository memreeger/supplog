package com.supplog.dto.routine;

import com.supplog.enums.Period;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutinePeriodRequestDto {
    @NotNull(message = "{validation.routine.period.required}")
    private Period period;
}
