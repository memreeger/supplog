package com.supplog.service.routineExecution.impl;

import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;
import com.supplog.entity.Routine;
import com.supplog.entity.RoutineExecution;
import com.supplog.entity.User;
import com.supplog.enums.Frequency;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.routine.RoutineScheduleMatcher;
import com.supplog.service.routineExecution.RoutineExecutionService;
import com.supplog.service.user.ActiveUserService;
import com.supplog.util.TimeZoneResolver;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutineExecutionServiceImpl implements RoutineExecutionService {

    private static final int MAX_LOOKBACK_DAYS = 7;

    private final RoutineExecutionRepository routineExecutionRepository;
    private final RoutineRepository routineRepository;
    private final RoutineScheduleMatcher routineScheduleMatcher;
    private final TimeZoneResolver timeZoneResolver;
    private final ActiveUserService activeUserService;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public List<RoutineExecutionResponseDto> getToday(Long currentUserId) {
        User user = activeUserService.getRequiredActiveUser(currentUserId);
        ZoneId zoneId = timeZoneResolver.resolve(user);
        LocalDate today = Instant.now().atZone(zoneId).toLocalDate();

        // GET /today yalnızca bugüne ait eksik kayıtları oluşturur; geçmiş günler için geriye dönük kayıt oluşturmaz.
        synchronizeToday(user.getId(), today, zoneId);

        return routineExecutionRepository
                .findActiveRoutineExecutionsForDate(currentUserId, today)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public Page<RoutineExecutionResponseDto> getHistory(
            Long currentUserId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        User user = activeUserService.getRequiredActiveUser(currentUserId);

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("routine.execution.date.range.invalid");
        }

        Page<RoutineExecution> executions;

        if (dateFrom == null && dateTo == null) {
            executions = routineExecutionRepository.findUserHistory(
                    currentUserId,
                    pageable
            );
        } else if (dateFrom != null && dateTo == null) {
            executions = routineExecutionRepository.findUserHistoryFrom(
                    currentUserId,
                    dateFrom,
                    pageable
            );
        } else if (dateFrom == null) {
            executions = routineExecutionRepository.findUserHistoryTo(
                    currentUserId,
                    dateTo,
                    pageable
            );
        } else {
            executions = routineExecutionRepository.findUserHistoryBetween(
                    currentUserId,
                    dateFrom,
                    dateTo,
                    pageable
            );
        }

        return executions.map(this::toResponseDto);
    }

    @Override
    @Transactional
    public RoutineExecutionResponseDto completeToday(Long currentUserId, Long routineId) {
        return resolveToday(currentUserId, routineId, RoutineExecutionStatus.COMPLETED);
    }

    @Override
    @Transactional
    public RoutineExecutionResponseDto skipToday(Long currentUserId, Long routineId) {
        return resolveToday(currentUserId, routineId, RoutineExecutionStatus.SKIPPED);
    }

    @Override
    @Transactional
    public void synchronizePendingAfterRoutineUpdate(Routine routine) {
        ZoneId zoneId = timeZoneResolver.resolve(routine.getUser());
        routineExecutionRepository
                .findAllByRoutineIdAndStatus(
                        routine.getId(),
                        RoutineExecutionStatus.PENDING
                )
                .forEach(execution -> {
                    if (!routineScheduleMatcher.occursOn(
                            routine,
                            execution.getScheduledDate()
                    )) {
                        routineExecutionRepository.delete(execution);
                        return;
                    }

                    ZonedDateTime scheduledDateTime = ZonedDateTime.of(
                            execution.getScheduledDate(),
                            routine.getRoutineTime(),
                            zoneId
                    );

                    execution.setScheduledTime(routine.getRoutineTime());
                    execution.setScheduledZoneId(zoneId.getId());
                    execution.setScheduledAt(scheduledDateTime.toInstant());
                    execution.setMissedAt(calculateMissedAt(
                            execution.getScheduledAt(),
                            routine
                    ));
                });
    }

    @Override
    @Transactional
    public void synchronizePendingAfterUserTimeZoneUpdate(User user) {
        ZoneId zoneId = timeZoneResolver.resolve(user);

        routineExecutionRepository
                .findAllByRoutineUserIdAndStatus(
                        user.getId(),
                        RoutineExecutionStatus.PENDING
                )
                .forEach(execution -> {
                    execution.setScheduledZoneId(zoneId.getId());
                    Instant scheduledAt =
                            ZonedDateTime.of(
                                    execution.getScheduledDate(),
                                    execution.getScheduledTime(),
                                    zoneId
                            ).toInstant();
                    execution.setScheduledAt(scheduledAt);
                    execution.setMissedAt(calculateMissedAt(
                            scheduledAt,
                            execution.getRoutine()
                    ));
                });
    }

    @Override
    @Transactional
    public void handleRoutineSoftDelete(Routine routine) {
        ZoneId zoneId = timeZoneResolver.resolve(routine.getUser());
        LocalDate today = LocalDate.now(zoneId);

        routineExecutionRepository.deletePendingFromDate(
                routine.getId(),
                RoutineExecutionStatus.PENDING,
                today
        );
    }

    private RoutineExecutionResponseDto resolveToday(
            Long currentUserId,
            Long routineId,
            RoutineExecutionStatus targetStatus
    ) {
        User user = activeUserService.getRequiredActiveUser(currentUserId);

        // Execution oluşturan bütün akışlar önce ilgili Routine kaydını kilitler.
        Routine routine = routineRepository
                .findActiveByIdAndUserIdForUpdate(routineId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("routine.not.found"));

        // Entity persistence context içinde önceden bulunabilir; kilit alındıktan sonra yeniden yüklenir.
        entityManager.refresh(routine);

        if (routine.getFrequency() == Frequency.AS_NEEDED) {
            throw new BusinessException("routine.execution.as.needed.not.supported");
        }

        ZoneId zoneId = timeZoneResolver.resolve(user);
        Instant now = Instant.now();
        LocalDate today = now.atZone(zoneId).toLocalDate();

        if (!routineScheduleMatcher.occursOn(routine, today)) {
            throw new BusinessException("routine.execution.not.scheduled.today");
        }

        RoutineExecution execution = routineExecutionRepository
                .findByRoutineAndDateForUpdate(routine.getId(), today)
                .orElseGet(() -> createPendingExecution(routine, today, zoneId));

        if (execution.getStatus() != RoutineExecutionStatus.PENDING) {
            throw new BusinessException("routine.execution.already.resolved");
        }

        if (!now.isBefore(execution.getMissedAt())) {
            throw new BusinessException("routine.execution.deadline.passed");
        }

        execution.setStatus(targetStatus);
        execution.setResolvedAt(now);

        return toResponseDto(routineExecutionRepository.save(execution));
    }

    @Scheduled(
            fixedDelayString = "${app.routine-execution.lifecycle-interval-ms:900000}",
            initialDelayString = "${app.routine-execution.lifecycle-initial-delay-ms:10000}"
    )
    public void synchronizeExecutionLifecycle() {
        Instant now = Instant.now();

        for (Long userId : userRepository.findAllActiveIds()) {
            try {
                transactionTemplate.executeWithoutResult(status ->
                        userRepository.findByIdAndIsDeletedFalse(userId)
                                .ifPresent(user -> synchronizeUserLifecycle(user, now)));
            } catch (Exception e) {
                log.error("Routine execution synchronization failed for userId={}", userId, e);
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${app.routine-execution.missed-interval-ms:60000}",
            initialDelayString = "${app.routine-execution.missed-initial-delay-ms:60000}"
    )
    @Transactional
    public void markExpiredPendingExecutionsAsMissed() {
        Instant now = Instant.now();
        routineExecutionRepository.markExpiredPendingAsMissed(
                now,
                LocalDateTime.ofInstant(now, ZoneId.of("UTC"))
        );
    }

    private void synchronizeUserLifecycle(User user, Instant now) {
        ZoneId zoneId = timeZoneResolver.resolve(user);
        LocalDate today = now.atZone(zoneId).toLocalDate();
        LocalDate firstDate = today.minusDays(MAX_LOOKBACK_DAYS);

        // 1 sorgu: kullanıcının tüm aktif rutinleri
        List<Routine> routines = routineRepository
                .findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(user.getId())
                .stream()
                .filter(r -> r.getFrequency() != Frequency.AS_NEEDED)
                .toList();
        if (routines.isEmpty()) return;

        // 1 sorgu: kullanıcının tüm mevcut execution tarihleri
        Map<Long, Set<LocalDate>> existing = routineExecutionRepository
                .findExistingDates(user.getId(), firstDate, today)
                .stream()
                .collect(Collectors.groupingBy(
                        RoutineExecutionRepository.RoutineDate::routineId,
                        Collectors.mapping(RoutineExecutionRepository.RoutineDate::date, Collectors.toSet())));

        // Kilitsiz ön kontrol: sadece eksik kaydı olan rutinler kilitlenecek
        List<Long> routineIdsNeedingWork = routines.stream()
                .filter(r -> !datesToCreate(r, zoneId, firstDate, today, now,
                        existing.getOrDefault(r.getId(), Set.of())).isEmpty())
                .map(Routine::getId)
                .sorted()          // kilit sırası korunuyor
                .toList();

        for (Long routineId : routineIdsNeedingWork) {
            Routine routine = lockActiveRoutine(routineId, user.getId()); // refresh burada gerekli
            if (routine == null) continue;

            // Kilit altında tekrar doğrula (güncel rutin kuralı + güncel kayıtlar)
            Set<LocalDate> existingNow = routineExecutionRepository
                    .findExistingDatesByRoutine(routineId, firstDate, today);

            for (LocalDate date : datesToCreate(routine, zoneId, firstDate, today, now, existingNow)) {
                routineExecutionRepository.save(createPendingExecution(routine, date, zoneId));
            }
        }
    }
    private void synchronizeToday(Long userId, LocalDate today, ZoneId zoneId) {
        Instant now = Instant.now();
        List<Routine> routines = routineRepository
                .findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(userId)
                .stream()
                .filter(r -> r.getFrequency() != Frequency.AS_NEEDED)
                .toList();

        Map<Long, Set<LocalDate>> existing = routineExecutionRepository
                .findExistingDates(userId, today, today).stream()
                .collect(Collectors.groupingBy(
                        RoutineExecutionRepository.RoutineDate::routineId,
                        Collectors.mapping(RoutineExecutionRepository.RoutineDate::date, Collectors.toSet())));

        routines.stream()
                .filter(r -> !datesToCreate(r, zoneId, today, today, now,
                        existing.getOrDefault(r.getId(), Set.of())).isEmpty())
                .map(Routine::getId).sorted()
                .forEach(routineId -> {
                    Routine routine = lockActiveRoutine(routineId, userId);
                    if (routine == null) return;
                    Set<LocalDate> existingNow = routineExecutionRepository
                            .findExistingDatesByRoutine(routineId, today, today);
                    datesToCreate(routine, zoneId, today, today, now, existingNow)
                            .forEach(d -> routineExecutionRepository
                                    .save(createPendingExecution(routine, d, zoneId)));
                });
    }

    private List<Long> activeRoutineIds(Long userId) {
        return routineRepository
                .findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(userId)
                .stream()
                .map(Routine::getId)
                .distinct()
                .sorted()
                .toList();
    }

    private Routine lockActiveRoutine(Long routineId, Long userId) {
        Routine routine = routineRepository
                .findActiveByIdAndUserIdForUpdate(routineId, userId)
                .orElse(null);

        if (routine != null) {
            // Kilit alındıktan sonra zamanlama, daysOfWeek ve denetim alanları yeniden yüklenir.
            entityManager.refresh(routine);
        }
        return routine;
    }

    private Instant lastKnownRoutineChange(Routine routine, Instant fallback) {
        // Spring Data auditing LocalDateTime değerlerini UTC olarak yazar.
        ZoneId auditZone = ZoneId.of("UTC");
        Instant createdAt = routine.getCreatedAt() == null
                ? fallback
                : routine.getCreatedAt().atZone(auditZone).toInstant();
        Instant updatedAt = routine.getUpdatedAt() == null
                ? createdAt
                : routine.getUpdatedAt().atZone(auditZone).toInstant();

        return updatedAt.isAfter(createdAt) ? updatedAt : createdAt;
    }

    private RoutineExecution createPendingExecution(
            Routine routine,
            LocalDate scheduledDate,
            ZoneId zoneId
    ) {
        RoutineExecution execution = new RoutineExecution();
        execution.setRoutine(routine);
        execution.setScheduledDate(scheduledDate);
        execution.setScheduledTime(routine.getRoutineTime());
        execution.setScheduledZoneId(zoneId.getId());
        Instant scheduledAt = ZonedDateTime.of(
                scheduledDate,
                routine.getRoutineTime(),
                zoneId
        ).toInstant();
        execution.setScheduledAt(scheduledAt);
        execution.setMissedAt(calculateMissedAt(scheduledAt, routine));
        execution.setStatus(RoutineExecutionStatus.PENDING);
        return execution;
    }

    private Instant calculateMissedAt(Instant scheduledAt, Routine routine) {
        return scheduledAt.plusSeconds(
                routine.getMissedGracePeriod().getMinutes() * 60L
        );
    }

    private RoutineExecutionResponseDto toResponseDto(RoutineExecution execution) {
        return new RoutineExecutionResponseDto(
                execution.getId(),
                execution.getRoutine().getId(),
                execution.getScheduledDate(),
                execution.getScheduledTime(),
                execution.getScheduledZoneId(),
                execution.getScheduledAt(),
                execution.getMissedAt(),
                execution.getStatus(),
                execution.getResolvedAt()
        );
    }

    private List<LocalDate> datesToCreate(
            Routine routine, ZoneId zoneId, LocalDate from, LocalDate today,
            Instant now, Set<LocalDate> existing
    ) {
        LocalDate date = routine.getStartDate().isAfter(from) ? routine.getStartDate() : from;
        Instant backfillCutoff = lastKnownRoutineChange(routine, now);
        List<LocalDate> result = new ArrayList<>();

        for (; !date.isAfter(today); date = date.plusDays(1)) {
            if (existing.contains(date) || !routineScheduleMatcher.occursOn(routine, date)) {
                continue;
            }
            boolean isPast = date.isBefore(today);
            Instant scheduledAt = ZonedDateTime
                    .of(date, routine.getRoutineTime(), zoneId).toInstant();

            // mevcut backfill kuralın aynen korunuyor
            if (isPast && scheduledAt.isBefore(backfillCutoff)) {
                continue;
            }
            result.add(date);
        }
        return result;
    }
}
