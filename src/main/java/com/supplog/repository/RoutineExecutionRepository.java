package com.supplog.repository;

import com.supplog.entity.RoutineExecution;
import com.supplog.enums.RoutineExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoutineExecutionRepository
        extends JpaRepository<RoutineExecution, Long> {

    Optional<RoutineExecution>
    findByRoutine_IdAndScheduledDate(
            Long routineId,
            LocalDate scheduledDate
    );

    @EntityGraph(attributePaths = {"routine", "routine.user"})
    List<RoutineExecution> findAllByRoutineIdOrderByScheduledAtDesc(Long routineId);

    @EntityGraph(attributePaths = {"routine", "routine.user"})
    List<RoutineExecution> findTop20ByRoutineUserIdOrderByScheduledAtDesc(Long userId);

    @EntityGraph(attributePaths = {"routine", "routine.user"})
    @Query("""
            SELECT e
            FROM RoutineExecution e
            WHERE (:userId IS NULL OR e.routine.user.id = :userId)
              AND (:routineId IS NULL OR e.routine.id = :routineId)
              AND (:dateFrom IS NULL OR e.scheduledDate >= :dateFrom)
              AND (:dateTo IS NULL OR e.scheduledDate <= :dateTo)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.scheduledAt DESC
            """)
    List<RoutineExecution> searchAdminExecutions(
            @Param("userId") Long userId,
            @Param("routineId") Long routineId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("status") RoutineExecutionStatus status
    );

    long countByScheduledDateAndStatus(
            LocalDate scheduledDate,
            RoutineExecutionStatus status
    );
}
