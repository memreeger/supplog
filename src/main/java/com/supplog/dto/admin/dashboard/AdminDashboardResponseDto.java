package com.supplog.dto.admin.dashboard;

import com.supplog.dto.admin.audit.AdminAuditResponseDto;

import java.util.List;

public record AdminDashboardResponseDto(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long totalAdmins,
        long activeSupplements,
        long inactiveSupplements,
        long activeRoutines,
        long inactiveRoutines,
        long completedToday,
        long skippedToday,
        long missedToday,
        long pendingSupportRequests,
        List<AdminAuditResponseDto> recentAdminActions
) {
}
