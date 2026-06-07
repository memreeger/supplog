package com.supplog.service.routine;

import com.supplog.dto.routine.CreateRoutineRequestDto;
import com.supplog.dto.routine.ResponseRoutineDto;
import com.supplog.entity.Routine;

import java.util.List;

public interface RoutineService {
    void addRoutine(CreateRoutineRequestDto routineRequestDto);

    List<ResponseRoutineDto> getAllRoutinesByUserId(Long userId);

    //crud update olmasın yeni rutin eklesin !
}
