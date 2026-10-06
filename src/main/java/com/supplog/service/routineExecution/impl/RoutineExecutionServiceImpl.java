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
import java.util.List;

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

        Instant now = Instant.now();
        LocalDate today = now.atZone(timeZoneResolver.resolve(user)).toLocalDate();

        // Scheduler beklenmeden geçmişteki PENDING kayıtları MISSED durumuna geçirilir.
        // Repository güncellemesi yalnızca geçmiş tarihlerdeki PENDING kayıtlarını hedefler.
        markExistingPastPendingAsMissed(currentUserId, today, now);

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
        LocalDate today = LocalDate.now(zoneId);

        routineExecutionRepository
                .findAllByRoutineIdAndStatusAndScheduledDateGreaterThanEqual(
                        routine.getId(),
                        RoutineExecutionStatus.PENDING,
                        today
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
                    execution.setScheduledAt(
                            ZonedDateTime.of(
                                    execution.getScheduledDate(),
                                    execution.getScheduledTime(),
                                    zoneId
                            ).toInstant()
                    );
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

        // Kullanıcı bazlı transaction dışında yönetilen User entity'leri yerine yalnızca kullanıcı ID'leri alınır.
        List<Long> userIds = userRepository.findAllByIsDeletedFalse()
                .stream()
                .map(User::getId)
                .toList();

        for (Long userId : userIds) {
            try {
                // Scheduler metodunda @Transactional yoktur; her kullanıcı ayrı bir transaction içinde işlenir.
                transactionTemplate.executeWithoutResult(status ->
                        userRepository.findByIdAndIsDeletedFalse(userId)
                                .ifPresent(user -> synchronizeUserLifecycle(user, now))
                );
            } catch (Exception exception) {
                // Hata oluşursa yalnızca ilgili kullanıcının senkronizasyon işlemi geri alınır.
                log.error("Routine execution synchronization failed for userId={}",
                        userId, exception);
            }
        }
    }

    private void synchronizeUserLifecycle(User user, Instant now) {
        ZoneId zoneId = timeZoneResolver.resolve(user);
        LocalDate today = now.atZone(zoneId).toLocalDate();

        // Bu UPDATE yalnızca geçmiş tarihlerde mevcut olan PENDING kayıtlarını değiştirir.
        // Bugünün complete/skip akışı geçmiş tarihleri çözümleyemez. Veritabanı satır kilitleri,
        // aynı execution üzerindeki eşzamanlı admin düzeltmelerini sıraya koyar.
        markExistingPastPendingAsMissed(user.getId(), today, now);

        // Geriye dönük kayıt oluşturma en fazla önceki yedi yerel günü ve bugünü kapsar.
        LocalDate firstDate = today.minusDays(MAX_LOOKBACK_DAYS);
        for (Long routineId : activeRoutineIds(user.getId())) {
            Routine routine = lockActiveRoutine(routineId, user.getId());
            if (routine == null || routine.getFrequency() == Frequency.AS_NEEDED) {
                continue;
            }

            LocalDate currentDate = routine.getStartDate().isAfter(firstDate)
                    ? routine.getStartDate()
                    : firstDate;

            // Rutin zamanlamasının sürüm geçmişi tutulmadığı için son değişiklikten önceki günlere
            // mevcut kuralları uygulayıp geriye dönük MISSED kaydı üretilmez.
            Instant backfillCutoff = lastKnownRoutineChange(routine, now);

            while (!currentDate.isAfter(today)) {
                if (routineScheduleMatcher.occursOn(routine, currentDate)) {
                    boolean isPast = currentDate.isBefore(today);
                    RoutineExecution candidate = createPendingExecution(routine, currentDate, zoneId);

                    // Planlanan saatten sonra oluşturulan veya güncellenen rutin, geçmişe dönük kaçırılmış sayılmaz.
                    // Kullanıcı bugünkü plan saatinden sonra ayarlama yapsa bile bugünün kaydı oluşturulur.
                    if (!isPast || !candidate.getScheduledAt().isBefore(backfillCutoff)) {
                        boolean exists = routineExecutionRepository
                                .findByRoutine_IdAndScheduledDate(routine.getId(), currentDate)
                                .isPresent();

                        if (!exists) {
                            if (isPast) {
                                candidate.setStatus(RoutineExecutionStatus.MISSED);
                                candidate.setResolvedAt(now);
                            }
                            routineExecutionRepository.save(candidate);
                        }
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }
    }

    private void synchronizeToday(Long userId, LocalDate today, ZoneId zoneId) {
        for (Long routineId : activeRoutineIds(userId)) {
            Routine routine = lockActiveRoutine(routineId, userId);
            if (routine == null || routine.getFrequency() == Frequency.AS_NEEDED
                    || !routineScheduleMatcher.occursOn(routine, today)) {
                continue;
            }

            // Execution kaydının varlığı yalnızca Routine kilidi alındıktan sonra kontrol edilir.
            // Kayıt oluşturan bütün akışlar aynı üst kayıt kilidini ve sıralamayı kullanır.
            if (routineExecutionRepository
                    .findByRoutine_IdAndScheduledDate(routineId, today)
                    .isEmpty()) {
                routineExecutionRepository.save(createPendingExecution(routine, today, zoneId));
            }
        }
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

    private void markExistingPastPendingAsMissed(Long userId, LocalDate today, Instant now) {
        routineExecutionRepository.markPastPendingAsMissed(
                userId,
                today,
                RoutineExecutionStatus.PENDING,
                RoutineExecutionStatus.MISSED,
                now,
                LocalDateTime.ofInstant(now, ZoneId.of("UTC"))
        );
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
        execution.setScheduledAt(
                ZonedDateTime.of(scheduledDate, routine.getRoutineTime(), zoneId).toInstant()
        );
        execution.setStatus(RoutineExecutionStatus.PENDING);
        return execution;
    }

    private RoutineExecutionResponseDto toResponseDto(RoutineExecution execution) {
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
