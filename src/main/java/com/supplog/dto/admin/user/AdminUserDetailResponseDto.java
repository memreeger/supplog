package com.supplog.dto.admin.user;

import com.supplog.dto.admin.audit.AdminAuditResponseDto;
import com.supplog.dto.admin.execution.AdminRoutineExecutionResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.admin.supplement.AdminSupplementResponseDto;
import com.supplog.dto.admin.support.AdminSupportResponseDto;

import java.util.List;

public record AdminUserDetailResponseDto(
        AdminUserResponseDto user,
        List<AdminSupplementResponseDto> supplements,
        List<AdminRoutineResponseDto> routines,
        List<AdminRoutineExecutionResponseDto> recentExecutions,
        List<AdminSupportResponseDto> supportRelationships,
        List<AdminAuditResponseDto> recentAdminActions
) {
}
