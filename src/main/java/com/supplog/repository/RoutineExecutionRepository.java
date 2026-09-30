package com.supplog.repository;

import com.supplog.entity.RoutineExecution;
import com.supplog.enums.RoutineExecutionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoutineExecutionRepository
        extends JpaRepository<RoutineExecution, Long> {

    Optional<RoutineExecution>
    findByRoutine_IdAndScheduledDate(
            Long routineId,
            LocalDate scheduledDate
    );

    List<RoutineExecution> findAllByRoutineIdAndStatusAndScheduledDateGreaterThanEqual(
            Long routineId,
            RoutineExecutionStatus status,
            LocalDate scheduledDate
    );

    List<RoutineExecution> findAllByRoutineUserIdAndStatus(
            Long userId,
            RoutineExecutionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM RoutineExecution e
            WHERE e.routine.id = :routineId
              AND e.scheduledDate = :scheduledDate
            """)
    Optional<RoutineExecution> findByRoutineAndDateForUpdate(
            @Param("routineId") Long routineId,
            @Param("scheduledDate") LocalDate scheduledDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM RoutineExecution e
            WHERE e.id = :executionId
            """)
    Optional<RoutineExecution> findByIdForUpdate(
            @Param("executionId") Long executionId
    );

    @EntityGraph(attributePaths = "routine")
    @Query("""
            SELECT e
            FROM RoutineExecution e
            WHERE e.routine.user.id = :userId
              AND e.scheduledDate = :scheduledDate
              AND e.routine.isDeleted = false
              AND e.routine.supplement.isDeleted = false
            ORDER BY e.scheduledAt ASC
            """)
    List<RoutineExecution> findActiveRoutineExecutionsForDate(
            @Param("userId") Long userId,
            @Param("scheduledDate") LocalDate scheduledDate
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM RoutineExecution e
            WHERE e.routine.id = :routineId
              AND e.status = :status
              AND e.scheduledDate >= :dateFrom
            """)
    int deletePendingFromDate(
            @Param("routineId") Long routineId,
            @Param("status") RoutineExecutionStatus status,
            @Param("dateFrom") LocalDate dateFrom
    );

    @EntityGraph(attributePaths = "routine")
    @Query("""
            SELECT e
            FROM RoutineExecution e
            WHERE e.routine.user.id = :userId
              AND (:dateFrom IS NULL OR e.scheduledDate >= :dateFrom)
              AND (:dateTo IS NULL OR e.scheduledDate <= :dateTo)
            ORDER BY e.scheduledAt DESC
            """)
    Page<RoutineExecution> findUserHistory(
            @Param("userId") Long userId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RoutineExecution e
            SET e.status = :missedStatus,
                e.resolvedAt = :resolvedAt,
                e.updatedAt = :updatedAt
            WHERE e.routine.user.id = :userId
              AND e.status = :pendingStatus
              AND e.scheduledDate < :today
            """)
    int markPastPendingAsMissed(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("pendingStatus") RoutineExecutionStatus pendingStatus,
            @Param("missedStatus") RoutineExecutionStatus missedStatus,
            @Param("resolvedAt") Instant resolvedAt,
            @Param("updatedAt") LocalDateTime updatedAt
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
    Page<RoutineExecution> searchAdminExecutions(
            @Param("userId") Long userId,
            @Param("routineId") Long routineId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("status") RoutineExecutionStatus status,
            Pageable pageable
    );

    long countByScheduledDateAndStatus(
            LocalDate scheduledDate,
            RoutineExecutionStatus status
    );
}
