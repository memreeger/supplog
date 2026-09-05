package com.supplog.service.routineExecution.impl;

import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;
import com.supplog.entity.Routine;
import com.supplog.entity.RoutineExecution;
import com.supplog.enums.Frequency;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.service.routine.RoutineScheduleMatcher;
import com.supplog.service.routineExecution.RoutineExecutionService;
import com.supplog.util.TimeZoneResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@Transactional(readOnly = true)
public class RoutineExecutionServiceImpl
        implements RoutineExecutionService {

    private final RoutineExecutionRepository routineExecutionRepository;
    private final RoutineRepository routineRepository;
    private final RoutineScheduleMatcher routineScheduleMatcher;
    private final TimeZoneResolver timeZoneResolver;

    public RoutineExecutionServiceImpl(
            RoutineExecutionRepository routineExecutionRepository,
            RoutineRepository routineRepository,
            RoutineScheduleMatcher routineScheduleMatcher,
            TimeZoneResolver timeZoneResolver
    ) {
        this.routineExecutionRepository = routineExecutionRepository;
        this.routineRepository = routineRepository;
        this.routineScheduleMatcher = routineScheduleMatcher;
        this.timeZoneResolver = timeZoneResolver;
    }

    @Override
    @Transactional
    public RoutineExecutionResponseDto completeToday(
            Long currentUserId,
            Long routineId
    ) {

        return resolveToday(
                currentUserId,
                routineId,
                RoutineExecutionStatus.COMPLETED
        );
    }

    @Override
    @Transactional
    public RoutineExecutionResponseDto skipToday(
            Long currentUserId,
            Long routineId
    ) {

        return resolveToday(
                currentUserId,
                routineId,
                RoutineExecutionStatus.SKIPPED
        );
    }

    private RoutineExecutionResponseDto resolveToday(
            Long currentUserId,
            Long routineId,
            RoutineExecutionStatus targetStatus
    ) {

        Routine routine =
                findMyActiveRoutine(
                        currentUserId,
                        routineId
                );

        if (routine.getFrequency() == Frequency.AS_NEEDED) {

            throw new BusinessException(
                    "routine.execution.as.needed.not.supported"
            );
        }

        ZoneId zoneId =
                timeZoneResolver.resolve(
                        routine.getUser()
                );

        Instant now = Instant.now();

        LocalDate today =
                now.atZone(zoneId)
                        .toLocalDate();

        if (!routineScheduleMatcher
                .occursOn(routine, today)) {

            throw new BusinessException(
                    "routine.execution.not.scheduled.today"
            );
        }

        RoutineExecution execution =
                routineExecutionRepository
                        .findByRoutine_IdAndScheduledDate(
                                routineId,
                                today
                        )
                        .orElseGet(() ->
                                createPendingExecution(
                                        routine,
                                        today,
                                        zoneId
                                )
                        );

        transitionToResolved(
                execution,
                targetStatus,
                now
        );

        RoutineExecution saved =
                routineExecutionRepository.save(
                        execution
                );

        return toResponseDto(saved);
    }

    private RoutineExecution createPendingExecution(
            Routine routine,
            LocalDate scheduledDate,
            ZoneId zoneId
    ) {

        ZonedDateTime scheduledDateTime =
                ZonedDateTime.of(
                        scheduledDate,
                        routine.getRoutineTime(),
                        zoneId
                );

        RoutineExecution execution =
                new RoutineExecution();

        execution.setRoutine(routine);

        execution.setScheduledDate(
                scheduledDate
        );

        execution.setScheduledTime(
                routine.getRoutineTime()
        );

        execution.setScheduledZoneId(
                zoneId.getId()
        );

        execution.setScheduledAt(
                scheduledDateTime.toInstant()
        );

        execution.setStatus(
                RoutineExecutionStatus.PENDING
        );

        execution.setResolvedAt(null);

        return execution;
    }

    private void transitionToResolved(
            RoutineExecution execution,
            RoutineExecutionStatus targetStatus,
            Instant now
    ) {

        if (execution.getStatus()
                != RoutineExecutionStatus.PENDING) {

            throw new BusinessException(
                    "routine.execution.already.resolved"
            );
        }

        if (targetStatus
                != RoutineExecutionStatus.COMPLETED
                && targetStatus
                != RoutineExecutionStatus.SKIPPED) {

            throw new BusinessException(
                    "routine.execution.invalid.transition"
            );
        }

        execution.setStatus(
                targetStatus
        );

        execution.setResolvedAt(
                now
        );
    }

    private Routine findMyActiveRoutine(
            Long currentUserId,
            Long routineId
    ) {

        return routineRepository
                .findByIdAndUserIdAndIsDeletedFalse(
                        routineId,
                        currentUserId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "routine.not.found",
                                routineId
                        )
                );
    }

    private RoutineExecutionResponseDto toResponseDto(
            RoutineExecution execution
    ) {

        return new RoutineExecutionResponseDto(
                execution.getId(),
                execution.getRoutine().getId(),
                execution.getScheduledDate(),
                execution.getScheduledTime(),
                execution.getScheduledZoneId(),
                execution.getScheduledAt(),
                execution.getStatus(),
                execution.getResolvedAt()
        );
    }
}