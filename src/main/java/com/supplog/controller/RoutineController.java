package com.supplog.controller;

import com.supplog.dto.routine.*;
import com.supplog.service.routine.RoutineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public void addRoutine(@Valid @RequestBody CreateRoutineRequestDto routineRequestDto) {
        routineService.addRoutine(routineRequestDto);
    }


    @GetMapping("/users/{userId}")
    public List<ResponseRoutineDto> getAllRoutinesByUserId(@PathVariable Long userId) {
        return routineService.getAllRoutinesByUserId(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoutineById(@PathVariable Long id) {
        routineService.deleteRoutine(id);
    }

    @PatchMapping("/{id}/time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineTime(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoutineTimeRequestDto requestDto) {
        routineService.updateRoutineTime(id, requestDto);
    }

    @PatchMapping("/{id}/day")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutineDay(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoutineDayRequestDto requestDto) {
        routineService.updateRoutineDay(id, requestDto);
    }

    @PatchMapping("/{id}/period")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRoutinePeriod(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoutinePeriodRequestDto requestDto) {
        routineService.updateRoutinePeriod(id, requestDto);
    }
}
