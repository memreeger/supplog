package com.supplog.controller;

import com.supplog.dto.routine.*;
import com.supplog.service.routine.RoutineService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId
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
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId
    ) {
        routineService.deleteRoutine(
                currentUser.getId(),
                routineId
        );
    }

    @PutMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutine(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody UpdateRoutineRequestDto requestDto
    ) {
        routineService.updateRoutine(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineTime(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody UpdateRoutineTimeRequestDto requestDto
    ) {
        routineService.updateRoutineTime(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/days")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineDays(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody UpdateRoutineDaysRequestDto requestDto
    ) {
        routineService.updateRoutineDays(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/frequency")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineFrequency(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody UpdateRoutineFrequencyRequestDto requestDto
    ) {
        routineService.updateRoutineFrequency(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/day-of-month")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineDayOfMonth(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody UpdateRoutineDayOfMonthRequestDto requestDto
    ) {
        routineService.updateRoutineDayOfMonth(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }

    @PatchMapping("/{routineId}/duration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineDuration(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable
            @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody UpdateRoutineDurationRequestDto requestDto
    ) {
        routineService.updateRoutineDuration(
                currentUser.getId(),
                routineId,
                requestDto
        );
    }
}
