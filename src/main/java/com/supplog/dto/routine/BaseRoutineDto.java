package com.supplog.dto.routine;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BaseRoutineDto {
    @NotNull(message = "{validation.supplementId.required}")
    @Positive(message = "{validation.id.positive}")
    private Long supplementId;

}
