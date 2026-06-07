package com.supplog.dto.routine;

import com.supplog.entity.Supplement;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseRoutineDto extends BaseRoutineDto {
    private Supplement supplement;
    private String dayName;
    private int hour;
    private int minute;
}
