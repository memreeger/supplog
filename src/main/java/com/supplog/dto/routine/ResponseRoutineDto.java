package com.supplog.dto.routine;

import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import lombok.*;

import java.time.LocalTime;
import java.time.Period;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseRoutineDto extends BaseRoutineDto {
    private Long id;
    private Long userId;
    private Long supplementId;
    private String supplementName;
    private String dayName;
    private LocalTime routineTime;
    private Period period;
}
