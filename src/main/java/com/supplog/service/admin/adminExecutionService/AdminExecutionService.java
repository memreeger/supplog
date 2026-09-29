package com.supplog.service.admin.adminExecutionService;

import com.supplog.dto.admin.execution.AdminExecutionCorrectionRequestDto;
import com.supplog.dto.admin.execution.AdminRoutineExecutionResponseDto;
import com.supplog.entity.RoutineExecution;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.service.admin.audit.AdminAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminExecutionService {

    private final RoutineExecutionRepository executionRepository;
    private final AdminAuditService auditService;

    public AdminExecutionService(
            RoutineExecutionRepository executionRepository,
            AdminAuditService auditService
    ) {
        this.executionRepository = executionRepository;
        this.auditService = auditService;
    }

    public List<AdminRoutineExecutionResponseDto> search(
            Long userId,
            Long routineId,
            LocalDate dateFrom,
            LocalDate dateTo,
            RoutineExecutionStatus status
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("admin.execution.date.range.invalid");
        }

        return executionRepository
                .searchAdminExecutions(userId, routineId, dateFrom, dateTo, status)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<AdminRoutineExecutionResponseDto> getRecentForUser(Long userId) {
        return executionRepository
                .findTop20ByRoutineUserIdOrderByScheduledAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AdminRoutineExecutionResponseDto correct(
            Long adminId,
            Long executionId,
            AdminExecutionCorrectionRequestDto request
    ) {
        RoutineExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "routine.execution.not.found",
                        executionId
                ));

        RoutineExecutionStatus previousStatus = execution.getStatus();
        if (previousStatus == request.status()) {
            throw new BusinessException("admin.execution.status.unchanged");
        }

        execution.setStatus(request.status());
        execution.setResolvedAt(
                request.status() == RoutineExecutionStatus.PENDING
                        ? null
                        : Instant.now()
        );

        auditService.record(
                adminId,
                "EXECUTION_STATUS_CORRECTED",
                "ROUTINE_EXECUTION",
                executionId,
                previousStatus.name(),
                request.status().name(),
                request.reason(),
                true,
                null
        );

        return toDto(execution);
    }

    public AdminRoutineExecutionResponseDto toDto(RoutineExecution execution) {
        return new AdminRoutineExecutionResponseDto(
                execution.getId(),
                execution.getRoutine().getId(),
                execution.getRoutine().getUser().getId(),
                execution.getScheduledDate(),
                execution.getScheduledTime(),
                execution.getScheduledZoneId(),
                execution.getScheduledAt(),
                execution.getStatus(),
                execution.getResolvedAt()
        );
    }
}
