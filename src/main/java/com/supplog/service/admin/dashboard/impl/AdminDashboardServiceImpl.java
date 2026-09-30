package com.supplog.service.admin.dashboard.impl;

import com.supplog.dto.admin.dashboard.AdminDashboardResponseDto;
import com.supplog.enums.RoleName;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.enums.SupportStatus;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.SupplementRepository;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.admin.audit.AdminAuditService;
import com.supplog.service.admin.dashboard.AdminDashboardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final UserRepository userRepository;
    private final SupplementRepository supplementRepository;
    private final RoutineRepository routineRepository;
    private final RoutineExecutionRepository executionRepository;
    private final SupportRelationshipRepository relationshipRepository;
    private final AdminAuditService auditService;
    private final ZoneId dashboardZoneId;

    public AdminDashboardServiceImpl(UserRepository userRepository,
            SupplementRepository supplementRepository, RoutineRepository routineRepository,
            RoutineExecutionRepository executionRepository,
            SupportRelationshipRepository relationshipRepository,
            AdminAuditService auditService,
            @Value("${app.time-zone:UTC}") String dashboardTimeZone) {
        this.userRepository = userRepository;
        this.supplementRepository = supplementRepository;
        this.routineRepository = routineRepository;
        this.executionRepository = executionRepository;
        this.relationshipRepository = relationshipRepository;
        this.auditService = auditService;
        this.dashboardZoneId = ZoneId.of(dashboardTimeZone);
    }

    @Override
    public AdminDashboardResponseDto getDashboard() {
        LocalDate today = LocalDate.now(dashboardZoneId);
        return new AdminDashboardResponseDto(userRepository.count(),
                userRepository.countByIsDeletedFalse(), userRepository.countByIsDeletedTrue(),
                userRepository.countUsersByRole(RoleName.ROLE_ADMIN),
                supplementRepository.countByIsDeletedFalse(), supplementRepository.countByIsDeletedTrue(),
                routineRepository.countByIsDeletedFalse(), routineRepository.countByIsDeletedTrue(),
                executionRepository.countByScheduledDateAndStatus(today, RoutineExecutionStatus.COMPLETED),
                executionRepository.countByScheduledDateAndStatus(today, RoutineExecutionStatus.SKIPPED),
                executionRepository.countByScheduledDateAndStatus(today, RoutineExecutionStatus.MISSED),
                relationshipRepository.countByStatus(SupportStatus.PENDING), auditService.getRecent());
    }
}
