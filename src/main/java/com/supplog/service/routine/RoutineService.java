package com.supplog.service.routine;

import com.supplog.dto.routine.*;

import java.util.List;

public interface RoutineService {
    void addRoutine(Long userId, CreateRoutineRequestDto routineRequestDto);

    List<RoutineResponseDto> getMyRoutines(Long userId);

    RoutineResponseDto getMyRoutineById(Long userId, Long routineId);

    void updateRoutine(Long userId, Long routineId, UpdateRoutineRequestDto requestDto);

    void deleteRoutine(Long userId, Long routineId);

    void updateRoutineTime(Long userId, Long routineId, UpdateRoutineTimeRequestDto requestDto
    );

    void updateRoutineDays(Long userId, Long routineId, UpdateRoutineDaysRequestDto requestDto);

    void updateRoutineFrequency(Long userId, Long routineId, UpdateRoutineFrequencyRequestDto requestDto);

    void updateRoutineDayOfMonth(Long userId, Long routineId, UpdateRoutineDayOfMonthRequestDto requestDto);

    void updateRoutineDuration(Long userId, Long routineId, UpdateRoutineDurationRequestDto requestDto);
}
