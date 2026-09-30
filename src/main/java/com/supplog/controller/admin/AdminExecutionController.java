package com.supplog.controller.admin;

import com.supplog.dto.admin.execution.AdminExecutionCorrectionRequestDto;
import com.supplog.dto.admin.execution.AdminRoutineExecutionResponseDto;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.service.admin.adminExecutionService.AdminExecutionService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/executions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExecutionController {

    private final AdminExecutionService service;

    public AdminExecutionController(AdminExecutionService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminRoutineExecutionResponseDto> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long routineId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) RoutineExecutionStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return service.search(userId, routineId, dateFrom, dateTo, status, pageable);
    }

    @PatchMapping("/{id}/status")
    public AdminRoutineExecutionResponseDto correct(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable @Positive Long id,
            @Valid @RequestBody AdminExecutionCorrectionRequestDto request
    ) {
        return service.correct(admin.getId(), id, request);
    }
}
