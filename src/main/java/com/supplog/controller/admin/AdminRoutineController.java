
package com.supplog.controller.admin;

import com.supplog.dto.admin.routine.AdminRoutineResponseDto;
import com.supplog.dto.routine.UpdateRoutineRequestDto;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public List<AdminRoutineResponseDto> getAllRoutines() {
        return adminRoutineService.getAll();
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
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id
    ) {
        adminRoutineService.activateRoutineById(id);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRoutine(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id
    ) {
        adminRoutineService.deactivateRoutineById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutine(
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long id,
            @Valid @RequestBody UpdateRoutineRequestDto requestDto
    ) {
        adminRoutineService.updateRoutineById(id, requestDto);
    }
}
