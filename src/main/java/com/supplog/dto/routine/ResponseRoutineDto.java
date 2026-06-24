package com.supplog.dto.routine;

import com.supplog.enums.Period;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseRoutineDto{
    private Long id;
    private Long userId;
    private Long supplementId;
    private String supplementName;
    private String dayName;
    private LocalTime routineTime;
    private Period period;
}
