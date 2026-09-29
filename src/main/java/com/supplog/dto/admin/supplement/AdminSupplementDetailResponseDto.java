package com.supplog.dto.admin.supplement;

import com.supplog.dto.admin.routine.AdminRoutineResponseDto;

import java.util.List;

public record AdminSupplementDetailResponseDto(
        AdminSupplementResponseDto supplement,
        List<AdminRoutineResponseDto> routines
) {
}
