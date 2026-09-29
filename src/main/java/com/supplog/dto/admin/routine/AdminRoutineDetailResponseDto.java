package com.supplog.dto.admin.routine;

import com.supplog.dto.admin.execution.AdminRoutineExecutionResponseDto;

import java.util.List;

public record AdminRoutineDetailResponseDto(
        AdminRoutineResponseDto routine,
        List<AdminRoutineExecutionResponseDto> executions
) {
}
