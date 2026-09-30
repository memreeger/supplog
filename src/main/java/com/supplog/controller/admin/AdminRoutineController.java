
package com.supplog.controller.admin;

import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineDetailResponseDto;
import com.supplog.dto.admin.routine.AdminRoutineUpdateRequestDto;
import com.supplog.dto.admin.AdminReasonRequestDto;
import com.supplog.enums.Frequency;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/routines")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoutineController {

    private final AdminRoutineService adminRoutineService;

    public AdminRoutineController(
            AdminRoutineService adminRoutineService
    ) {
        this.adminRoutineService = adminRoutineService;
    }

    @GetMapping
    public Page<AdminRoutineResponseDto> getAllRoutines(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long supplementId,
            @RequestParam(required = false) Frequency frequency,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminRoutineService.search(userId, supplementId, frequency, active, pageable);
    }

    @GetMapping("/{id}/detail")
    public AdminRoutineDetailResponseDto getDetail(
            @PathVariable @Positive(message = "{validation.id.positive}") Long id
    ) {
        return adminRoutineService.getDetail(id);
    }

    @GetMapping("/{id}")
    public AdminRoutineResponseDto getRoutineById(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id
    ) {
        return adminRoutineService.getById(id);
    }

    @GetMapping("/active")
    public List<AdminRoutineResponseDto> getAllActiveRoutines() {
        return adminRoutineService.getAllActiveRoutines();
    }

    @GetMapping("/inactive")
    public List<AdminRoutineResponseDto> getAllInactiveRoutines() {
        return adminRoutineService.getAllInactiveRoutines();
    }

    @GetMapping("/users/{userId}")
    public List<AdminRoutineResponseDto> getAllRoutinesByUserId(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long userId
    ) {
        return adminRoutineService.getAllRoutinesByUserId(userId);
    }

    @GetMapping("/supplements/{supplementId}")
    public List<AdminRoutineResponseDto> getAllRoutinesBySupplementId(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long supplementId
    ) {
        return adminRoutineService
                .getAllRoutinesBySupplementId(supplementId);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateRoutine(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        adminRoutineService.activateRoutineById(admin.getId(), id, request.reason());
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRoutine(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody AdminReasonRequestDto request
    ) {
        adminRoutineService.deactivateRoutineById(admin.getId(), id, request.reason());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutine(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody AdminRoutineUpdateRequestDto requestDto
    ) {
        adminRoutineService.updateRoutineById(admin.getId(), id, requestDto);
    }
}
