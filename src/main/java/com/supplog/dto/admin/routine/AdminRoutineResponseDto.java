package com.supplog.dto.admin.routine;

import com.supplog.dto.routine.RoutineResponseDto;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdminRoutineResponseDto extends RoutineResponseDto {
    private boolean deleted;
}
