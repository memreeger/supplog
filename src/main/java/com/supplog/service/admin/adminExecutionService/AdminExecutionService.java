package com.supplog.service.admin.adminExecutionService;

import com.supplog.dto.admin.execution.AdminExecutionCorrectionRequestDto;
import com.supplog.dto.admin.execution.AdminRoutineExecutionResponseDto;
import com.supplog.enums.RoutineExecutionStatus;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminExecutionService {
    Page<AdminRoutineExecutionResponseDto> search(Long userId, Long routineId,
            LocalDate dateFrom, LocalDate dateTo, RoutineExecutionStatus status,
            Pageable pageable);

    List<AdminRoutineExecutionResponseDto> getRecentForUser(Long userId);

    AdminRoutineExecutionResponseDto correct(Long adminId, Long executionId,
                                             AdminExecutionCorrectionRequestDto request);

}
