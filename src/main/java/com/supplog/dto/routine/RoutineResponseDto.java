package com.supplog.dto.routine;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supplog.enums.Period;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RoutineResponseDto {
    private Long id;
    private Long userId;
    private Long supplementId;
    private String supplementName;
    private String dayName;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime routineTime;
    private Period period;
}
