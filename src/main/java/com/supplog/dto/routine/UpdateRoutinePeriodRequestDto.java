package com.supplog.dto.routine;

import com.supplog.enums.Period;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutinePeriodRequestDto {
    private Period period;
}
