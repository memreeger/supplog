package com.supplog.service.admin.adminExecutionService.impl;

import com.supplog.dto.admin.execution.AdminExecutionCorrectionRequestDto;
import com.supplog.dto.admin.execution.AdminRoutineExecutionResponseDto;
import com.supplog.entity.RoutineExecution;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.service.admin.adminExecutionService.AdminExecutionService;
import com.supplog.service.admin.audit.AdminAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminExecutionServiceImpl implements AdminExecutionService {
    private final RoutineExecutionRepository executionRepository;
    private final RoutineRepository routineRepository;
    private final AdminAuditService auditService;

    public AdminExecutionServiceImpl(RoutineExecutionRepository executionRepository,
                                     RoutineRepository routineRepository,
                                     AdminAuditService auditService) {
        this.executionRepository = executionRepository;
        this.routineRepository = routineRepository;
        this.auditService = auditService;
    }

    @Override
    public Page<AdminRoutineExecutionResponseDto> search(Long userId, Long routineId,
            LocalDate dateFrom, LocalDate dateTo, RoutineExecutionStatus status,
            Pageable pageable) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("admin.execution.date.range.invalid");
        }
        return executionRepository.searchAdminExecutions(
                userId, routineId, dateFrom, dateTo, status, pageable).map(this::toDto);
    }

    @Override
    public List<AdminRoutineExecutionResponseDto> getRecentForUser(Long userId) {
        return executionRepository.findTop20ByRoutineUserIdOrderByScheduledAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public AdminRoutineExecutionResponseDto correct(Long adminId, Long executionId,
            AdminExecutionCorrectionRequestDto request) {
        RoutineExecution candidate = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "routine.execution.not.found", executionId));

        routineRepository.findByIdForUpdate(candidate.getRoutine().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "routine.not.found", candidate.getRoutine().getId()));

        RoutineExecution execution = executionRepository.findByIdForUpdate(executionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "routine.execution.not.found", executionId));
        RoutineExecutionStatus previousStatus = execution.getStatus();
        if (previousStatus == request.status()) {
            throw new BusinessException("admin.execution.status.unchanged");
        }
        execution.setStatus(request.status());
        execution.setResolvedAt(request.status() == RoutineExecutionStatus.PENDING
                ? null : Instant.now());
        auditService.record(adminId, "EXECUTION_STATUS_CORRECTED", "ROUTINE_EXECUTION",
                executionId, previousStatus.name(), request.status().name(),
                request.reason(), true, null);
        return toDto(execution);
    }

    private AdminRoutineExecutionResponseDto toDto(RoutineExecution execution) {
        return new AdminRoutineExecutionResponseDto(execution.getId(),
                execution.getRoutine().getId(), execution.getRoutine().getUser().getId(),
                execution.getScheduledDate(), execution.getScheduledTime(),
                execution.getScheduledZoneId(), execution.getScheduledAt(),
                execution.getMissedAt(),
                execution.getStatus(), execution.getResolvedAt());
    }
}
