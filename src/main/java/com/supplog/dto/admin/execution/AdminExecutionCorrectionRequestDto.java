package com.supplog.dto.admin.execution;

import com.supplog.enums.RoutineExecutionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminExecutionCorrectionRequestDto(
        @NotNull(message = "{validation.execution.status.required}")
        RoutineExecutionStatus status,
        @NotBlank(message = "{validation.admin.reason.required}")
        @Size(max = 500, message = "{validation.admin.reason.size}")
        String reason
) {
}
