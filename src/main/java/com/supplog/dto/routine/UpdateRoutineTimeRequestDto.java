package com.supplog.dto.routine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineTimeRequestDto {
    @Schema(type = "string", example = "08:30") // swaggerda local date girişi düzenlemek için
    private LocalTime routineTime;
}
