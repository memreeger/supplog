package com.supplog.dto.routine;

import com.supplog.enums.DayOfWeek;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineDayRequestDto {
    private DayOfWeek dayName;
}
