package com.supplog.dto.routine;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BaseRoutineDto {
    @NotNull(message = "{validation.supplementId.required}")
    private Long supplementId;

}
