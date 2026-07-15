package com.supplog.controller;

import com.supplog.dto.routine.*;
import com.supplog.service.routine.RoutineService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addRoutine(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateRoutineRequestDto requestDto
    ) {
        routineService.addRoutine(currentUser.getId(), requestDto);
    }

    @GetMapping
    public List<RoutineResponseDto> getMyRoutines(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return routineService.getMyRoutines(currentUser.getId());
    }

    @GetMapping("/{routineId}")
    public RoutineResponseDto getMyRoutineById(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long routineId
    ) {
        return routineService.getMyRoutineById(
                currentUser.getId(),
                routineId
        );
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoutine(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long routineId
    ) {
        routineService.deleteRoutine(
                currentUser.getId(),
                routineId
        );
    }

    @PatchMapping("/{routineId}/time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineTime(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long routineId,
            @Valid @RequestBody UpdateRoutineTimeRequestDto requestDto
    ) {
        routineService.updateRoutineTime(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/day")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineDay(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long routineId,
            @Valid @RequestBody UpdateRoutineDayRequestDto requestDto
    ) {
        routineService.updateRoutineDay(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/period")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutinePeriod(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long routineId,
            @Valid @RequestBody UpdateRoutinePeriodRequestDto requestDto
    ) {
        routineService.updateRoutinePeriod(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }
}
