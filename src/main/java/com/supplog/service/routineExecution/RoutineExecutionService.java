package com.supplog.service.routineExecution;

import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;
import com.supplog.entity.Routine;
import com.supplog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RoutineExecutionService {

    List<RoutineExecutionResponseDto> getToday(
            Long currentUserId
    );

    Page<RoutineExecutionResponseDto> getHistory(
            Long currentUserId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    );

    RoutineExecutionResponseDto completeToday(
            Long currentUserId,
            Long routineId
    );

    RoutineExecutionResponseDto skipToday(
            Long currentUserId,
            Long routineId
    );

    void synchronizePendingAfterRoutineUpdate(Routine routine);

    void synchronizePendingAfterUserTimeZoneUpdate(User user);

    void handleRoutineSoftDelete(Routine routine);
}
