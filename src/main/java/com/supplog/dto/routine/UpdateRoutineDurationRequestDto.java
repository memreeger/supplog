package com.supplog.dto.routine;

import com.supplog.enums.DurationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRoutineDurationRequestDto {

    @NotNull(message = "{validation.routine.durationType.required}")
    private DurationType durationType;

    private LocalDate startDate;

    private LocalDate endDate;
}