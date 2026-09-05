package com.supplog.repository;

import com.supplog.entity.RoutineExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RoutineExecutionRepository
        extends JpaRepository<RoutineExecution, Long> {

    Optional<RoutineExecution>
    findByRoutine_IdAndScheduledDate(
            Long routineId,
            LocalDate scheduledDate
    );
}