package com.supplog.service.routine;

import com.supplog.dto.routine.*;

import java.util.List;

public interface RoutineService {
    void addRoutine(Long userId, CreateRoutineRequestDto routineRequestDto);

    List<RoutineResponseDto> getMyRoutines(Long userId);

    RoutineResponseDto getMyRoutineById(Long userId, Long routineId);

    void updateRoutineTime(Long userId, Long routineId, UpdateRoutineTimeRequestDto requestDto);

    void updateRoutineDay(Long userId, Long routineId, UpdateRoutineDayRequestDto requestDto);

    void updateRoutinePeriod(Long userId, Long routineId, UpdateRoutinePeriodRequestDto requestDto);

    void deleteRoutine(Long userId, Long routineId);


    //crud update olmasın yeni rutin eklesin !
}
