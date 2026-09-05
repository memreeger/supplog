package com.supplog.service.routineExecution;

import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;

public interface RoutineExecutionService {

    RoutineExecutionResponseDto completeToday(
            Long currentUserId,
            Long routineId
    );

    RoutineExecutionResponseDto skipToday(
            Long currentUserId,
            Long routineId
    );
}