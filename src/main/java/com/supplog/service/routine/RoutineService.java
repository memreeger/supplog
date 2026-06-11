package com.supplog.service.routine;

import com.supplog.dto.routine.*;

import java.util.List;

public interface RoutineService {
    void addRoutine(CreateRoutineRequestDto routineRequestDto);

    List<ResponseRoutineDto> getAllRoutinesByUserId(Long userId);

    void deleteRoutine(Long id);

    void updateRoutineTime(Long id, UpdateRoutineTimeRequestDto requestDto);

    void updateRoutineDay(Long id, UpdateRoutineDayRequestDto requestDto);

    void updateRoutinePeriod(Long id, UpdateRoutinePeriodRequestDto requestDto);


    //crud update olmasın yeni rutin eklesin !
}
