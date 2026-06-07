package com.supplog.controller;

import com.supplog.dto.routine.CreateRoutineRequestDto;
import com.supplog.dto.routine.ResponseRoutineDto;
import com.supplog.entity.Routine;
import com.supplog.repository.RoutineRepository;
import com.supplog.service.routine.RoutineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/routine")
public class RoutineController {
    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @PostMapping
    public void addRoutine(@RequestBody CreateRoutineRequestDto routineRequestDto) {
        routineService.addRoutine(routineRequestDto);
    }

    @GetMapping
    public List<ResponseRoutineDto> getAllRoutinesByUserId(Long userId) {
        return routineService.getAllRoutinesByUserId(userId);
    }
}
