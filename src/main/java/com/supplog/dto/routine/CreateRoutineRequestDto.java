package com.supplog.dto.routine;

import com.supplog.enums.Period;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateRoutineRequestDto extends BaseRoutineDto {
    private String dayName;
    private int hour;
    private int minute;
    private Period period;
}
