
package com.supplog.controller.admin;

import com.supplog.dto.admin.routine.UpdateRoutineRequestDtoAdmin;
import com.supplog.dto.routine.RoutineResponseDto;
import com.supplog.service.admin.adminRoutineService.AdminRoutineService;
import jakarta.validation.Valid;
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
    public List<RoutineResponseDto> getAllRoutines() {
        return adminRoutineService.getAll();
    }

    @GetMapping("/{id}")
    public RoutineResponseDto getRoutineById(
            @PathVariable Long id
    ) {
        return adminRoutineService.getById(id);
    }

    @GetMapping("/active")
    public List<RoutineResponseDto> getAllActiveRoutines() {
        return adminRoutineService.getAllActiveRoutines();
    }

    @GetMapping("/inactive")
    public List<RoutineResponseDto> getAllInactiveRoutines() {
        return adminRoutineService.getAllInactiveRoutines();
    }

    @GetMapping("/users/{userId}")
    public List<RoutineResponseDto> getAllRoutinesByUserId(
            @PathVariable Long userId
    ) {
        return adminRoutineService.getAllRoutinesByUserId(userId);
    }

    @GetMapping("/supplements/{supplementId}")
    public List<RoutineResponseDto> getAllRoutinesBySupplementId(
            @PathVariable Long supplementId
    ) {
        return adminRoutineService
                .getAllRoutinesBySupplementId(supplementId);
    }

    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateRoutine(
            @PathVariable Long id
    ) {
        adminRoutineService.activateRoutineById(id);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRoutine(
            @PathVariable Long id
    ) {
        adminRoutineService.deactivateRoutineById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutine(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoutineRequestDtoAdmin requestDto
    ) {
        adminRoutineService.updateRoutineById(id, requestDto);
    }
}
