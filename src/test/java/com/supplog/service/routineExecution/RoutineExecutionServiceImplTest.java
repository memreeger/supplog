package com.supplog.service.routineExecution;

import com.supplog.entity.Routine;
import com.supplog.dto.routineExecution.RoutineExecutionResponseDto;
import com.supplog.entity.RoutineExecution;
import com.supplog.entity.Supplement;
import com.supplog.entity.User;
import com.supplog.enums.Frequency;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.enums.MissedGracePeriod;
import com.supplog.exception.BusinessException;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.routine.RoutineScheduleMatcher;
import com.supplog.service.routineExecution.impl.RoutineExecutionServiceImpl;
import com.supplog.service.user.ActiveUserService;
import com.supplog.util.TimeZoneResolver;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class RoutineExecutionServiceImplTest {
    @Mock
    RoutineExecutionRepository executionRepository;
    @Mock
    RoutineRepository routineRepository;
    @Mock
    RoutineScheduleMatcher scheduleMatcher;
    @Mock
    TimeZoneResolver timeZoneResolver;
    @Mock
    ActiveUserService activeUserService;
    @Mock
    UserRepository userRepository;
    @Mock
    TransactionTemplate transactionTemplate;
    @Mock
    EntityManager entityManager;
    @Mock
    NotificationService notificationService;

    private RoutineExecutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoutineExecutionServiceImpl(
                executionRepository, routineRepository, scheduleMatcher,
                timeZoneResolver, activeUserService, userRepository,
                transactionTemplate, entityManager, notificationService);
    }

    @Test
    void updatesPendingExecutionTimeAfterRoutineUpdate() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        Routine routine = routine(1L, user, LocalTime.of(9, 30));
        RoutineExecution execution = pending(routine, today, LocalTime.of(8, 0));

        when(timeZoneResolver.resolve(user)).thenReturn(zoneId);
        when(executionRepository.findAllByRoutineIdAndStatus(
                1L, RoutineExecutionStatus.PENDING)).thenReturn(List.of(execution));
        when(scheduleMatcher.occursOn(routine, today)).thenReturn(true);

        service.synchronizePendingAfterRoutineUpdate(routine);

        assertEquals(LocalTime.of(9, 30), execution.getScheduledTime());
        assertEquals(zoneId.getId(), execution.getScheduledZoneId());
        assertEquals(
                ZonedDateTime.of(today, LocalTime.of(9, 30), zoneId).toInstant().plusSeconds(1800),
                execution.getMissedAt()
        );
    }

    @Test
    void removesPendingExecutionWhenUpdatedRoutineNoLongerOccurs() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        Routine routine = routine(1L, user, LocalTime.of(9, 30));
        RoutineExecution execution = pending(routine, today, LocalTime.of(8, 0));

        when(timeZoneResolver.resolve(user)).thenReturn(zoneId);
        when(executionRepository.findAllByRoutineIdAndStatus(
                1L, RoutineExecutionStatus.PENDING)).thenReturn(List.of(execution));
        when(scheduleMatcher.occursOn(routine, today)).thenReturn(false);

        service.synchronizePendingAfterRoutineUpdate(routine);

        verify(executionRepository).delete(execution);
    }

    @Test
    void recalculatesOnlyPendingExecutionInstantAfterUserTimeZoneUpdate() {
        ZoneId newZone = ZoneId.of("America/New_York");
        LocalDate scheduledDate = LocalDate.of(2026, 9, 30);
        LocalTime scheduledTime = LocalTime.of(8, 30);
        User user = new User();
        user.setId(7L);
        Routine routine = routine(1L, user, scheduledTime);
        RoutineExecution execution = pending(routine, scheduledDate, scheduledTime);

        when(timeZoneResolver.resolve(user)).thenReturn(newZone);
        when(executionRepository.findAllByRoutineUserIdAndStatus(
                7L, RoutineExecutionStatus.PENDING)).thenReturn(List.of(execution));

        service.synchronizePendingAfterUserTimeZoneUpdate(user);

        assertEquals(newZone.getId(), execution.getScheduledZoneId());
        assertEquals(
                ZonedDateTime.of(scheduledDate, scheduledTime, newZone).toInstant(),
                execution.getScheduledAt()
        );
        assertEquals(execution.getScheduledAt().plusSeconds(1800), execution.getMissedAt());
    }

    @Test
    void recalculatesMissedAtForEveryGracePeriod() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate date = LocalDate.of(2026, 10, 6);
        User user = new User();
        Routine routine = routine(1L, user, LocalTime.of(9, 0));
        RoutineExecution execution = pending(routine, date, LocalTime.of(9, 0));

        when(timeZoneResolver.resolve(user)).thenReturn(zoneId);
        when(executionRepository.findAllByRoutineIdAndStatus(
                1L, RoutineExecutionStatus.PENDING))
                .thenReturn(List.of(execution));
        when(scheduleMatcher.occursOn(routine, date)).thenReturn(true);

        for (MissedGracePeriod gracePeriod : MissedGracePeriod.values()) {
            routine.setMissedGracePeriod(gracePeriod);
            service.synchronizePendingAfterRoutineUpdate(routine);
            assertEquals(
                    execution.getScheduledAt().plusSeconds(gracePeriod.getMinutes() * 60L),
                    execution.getMissedAt()
            );
        }
    }

    @Test
    void routineDefaultsToThirtyMinuteGracePeriod() {
        assertEquals(MissedGracePeriod.THIRTY_MINUTES, new Routine().getMissedGracePeriod());
    }

    @Test
    void missedSchedulerUsesSingleConditionalBulkUpdate() {
        service.markExpiredPendingExecutionsAsMissed();

        verify(executionRepository).markExpiredPendingAsMissed(
                any(Instant.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void rejectsResolutionAfterDeadlineBeforeSchedulerRuns() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        Routine routine = routine(1L, user, LocalTime.of(9, 0));
        routine.setFrequency(Frequency.DAILY);
        RoutineExecution execution = pending(routine, today, LocalTime.of(9, 0));
        execution.setMissedAt(Instant.now().minusSeconds(1));

        when(activeUserService.getRequiredActiveUser(7L)).thenReturn(user);
        when(routineRepository.findActiveByIdAndUserIdForUpdate(1L, 7L))
                .thenReturn(Optional.of(routine));
        when(timeZoneResolver.resolve(user)).thenReturn(zoneId);
        when(scheduleMatcher.occursOn(routine, today)).thenReturn(true);
        when(executionRepository.findByRoutineAndDateForUpdate(1L, today))
                .thenReturn(Optional.of(execution));

        assertThrows(BusinessException.class, () -> service.completeToday(7L, 1L));
        verify(executionRepository, never()).save(any());
    }

    @Test
    void completingADoseResolvesItsNotifications() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        Routine routine = routine(1L, user, LocalTime.of(9, 0));
        routine.setFrequency(Frequency.DAILY);
        RoutineExecution execution = pending(routine, today, LocalTime.of(9, 0));
        execution.setId(55L);
        execution.setMissedAt(Instant.now().plusSeconds(600));
        com.supplog.entity.Supplement supplement = new com.supplog.entity.Supplement();
        supplement.setName("Zinc");
        routine.setSupplement(supplement);

        when(activeUserService.getRequiredActiveUser(7L)).thenReturn(user);
        when(routineRepository.findActiveByIdAndUserIdForUpdate(1L, 7L))
                .thenReturn(Optional.of(routine));
        when(timeZoneResolver.resolve(user)).thenReturn(zoneId);
        when(scheduleMatcher.occursOn(routine, today)).thenReturn(true);
        when(executionRepository.findByRoutineAndDateForUpdate(1L, today))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(execution)).thenReturn(execution);

        service.completeToday(7L, 1L);

        verify(notificationService).resolveForExecution(55L);
    }

    @Test
    void downtimeRecoveryIncludesThreeDaysButExcludesEightDaysAndUsesUserZone() {
        ZoneId zoneId = ZoneId.of("Pacific/Auckland");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        Routine routine = routine(11L, user, LocalTime.NOON);
        routine.setStartDate(today.minusDays(30));
        routine.setCreatedAt(LocalDateTime.now().minusDays(20));
        routine.setUpdatedAt(routine.getCreatedAt());

        configureLifecycle(user, routine, zoneId);
        when(scheduleMatcher.occursOn(any(Routine.class), any(LocalDate.class))).thenReturn(true);

        service.synchronizeExecutionLifecycle();

        ArgumentCaptor<RoutineExecution> captor = ArgumentCaptor.forClass(RoutineExecution.class);
        verify(executionRepository, org.mockito.Mockito.times(8)).save(captor.capture());
        List<LocalDate> dates = captor.getAllValues().stream()
                .map(RoutineExecution::getScheduledDate).toList();
        assertFalse(dates.contains(today.minusDays(8)));
        assertEquals(true, dates.contains(today.minusDays(3)));
        assertEquals(today, dates.get(dates.size() - 1));
    }

    @Test
    void backdatedRoutineDoesNotCreatePastMissedExecutions() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        // 23:59: bugünkü dozun süresi testin çalıştığı saatten bağımsız olarak hep gelecekte.
        Routine routine = routine(11L, user, LocalTime.of(23, 59));
        routine.setStartDate(today.minusDays(30));
        routine.setCreatedAt(LocalDateTime.now(zoneId));
        routine.setUpdatedAt(routine.getCreatedAt());

        configureLifecycle(user, routine, zoneId);
        when(scheduleMatcher.occursOn(any(Routine.class), any(LocalDate.class))).thenReturn(true);

        service.synchronizeExecutionLifecycle();

        ArgumentCaptor<RoutineExecution> captor = ArgumentCaptor.forClass(RoutineExecution.class);
        verify(executionRepository).save(captor.capture());
        assertEquals(today, captor.getValue().getScheduledDate());
        assertEquals(RoutineExecutionStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void routineCreatedAfterTodaysWindowStartsTomorrow() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        // 00:00 dozu (süre 00:30'da biter), rutin gün sonunda oluşturulmuş.
        Routine routine = routine(11L, user, LocalTime.MIDNIGHT);
        routine.setStartDate(today);
        routine.setCreatedAt(today.atTime(23, 59));
        routine.setUpdatedAt(routine.getCreatedAt());

        configureLifecycle(user, routine, zoneId);
        when(scheduleMatcher.occursOn(any(Routine.class), any(LocalDate.class))).thenReturn(true);

        service.synchronizeExecutionLifecycle();

        verify(executionRepository, never()).save(any(RoutineExecution.class));
    }

    @Test
    void routineCreatedWithinGracePeriodKeepsTodaysDose() {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        // 23:00 dozu, 23:10'da eklenmiş: süre 23:30'a kadar açık, bugünkü doz kaçırılmış sayılmamalı.
        Routine routine = routine(11L, user, LocalTime.of(23, 0));
        routine.setStartDate(today);
        routine.setCreatedAt(today.atTime(23, 10));
        routine.setUpdatedAt(routine.getCreatedAt());

        configureLifecycle(user, routine, zoneId);
        when(scheduleMatcher.occursOn(any(Routine.class), any(LocalDate.class))).thenReturn(true);

        service.synchronizeExecutionLifecycle();

        ArgumentCaptor<RoutineExecution> captor = ArgumentCaptor.forClass(RoutineExecution.class);
        verify(executionRepository).save(captor.capture());
        assertEquals(today, captor.getValue().getScheduledDate());
    }

    @Test
    void undoReturnsResolvedDoseToPendingBeforeDeadline() {
        RoutineExecution execution = undoFixture(RoutineExecutionStatus.COMPLETED, Instant.now().plusSeconds(600));

        RoutineExecutionResponseDto response = service.undoToday(7L, 1L);

        assertEquals(RoutineExecutionStatus.PENDING, execution.getStatus());
        assertNull(execution.getResolvedAt());
        assertEquals(RoutineExecutionStatus.PENDING, response.getStatus());
        assertEquals("Vitamin D", response.getSupplementName());
        assertEquals("1000 IU", response.getSupplementDosage());
        assertFalse(response.isRoutineDeleted());
    }

    @Test
    void undoIsRejectedAfterDeadlineOrForPendingDose() {
        RoutineExecution late = undoFixture(RoutineExecutionStatus.SKIPPED, Instant.now().minusSeconds(1));
        BusinessException deadline = assertThrows(BusinessException.class, () -> service.undoToday(7L, 1L));
        assertEquals("routine.execution.undo.deadline.passed", deadline.getMessage());
        assertEquals(RoutineExecutionStatus.SKIPPED, late.getStatus());

        undoFixture(RoutineExecutionStatus.PENDING, Instant.now().plusSeconds(600));
        BusinessException pending = assertThrows(BusinessException.class, () -> service.undoToday(7L, 1L));
        assertEquals("routine.execution.not.resolved", pending.getMessage());
    }

    private RoutineExecution undoFixture(RoutineExecutionStatus status, Instant missedAt) {
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(zoneId);
        User user = new User();
        user.setId(7L);
        Supplement supplement = new Supplement();
        supplement.setName("Vitamin D");
        supplement.setSuppDosage("1000 IU");
        Routine routine = routine(1L, user, LocalTime.of(9, 0));
        routine.setSupplement(supplement);
        RoutineExecution execution = pending(routine, today, LocalTime.of(9, 0));
        execution.setStatus(status);
        execution.setResolvedAt(status == RoutineExecutionStatus.PENDING ? null : Instant.now());
        execution.setMissedAt(missedAt);

        when(activeUserService.getRequiredActiveUser(7L)).thenReturn(user);
        when(routineRepository.findActiveByIdAndUserIdForUpdate(1L, 7L)).thenReturn(Optional.of(routine));
        when(timeZoneResolver.resolve(user)).thenReturn(zoneId);
        when(executionRepository.findByRoutineAndDateForUpdate(1L, today)).thenReturn(Optional.of(execution));
        org.mockito.Mockito.lenient().when(executionRepository.save(execution)).thenReturn(execution);
        return execution;
    }

    @SuppressWarnings("unchecked")
    private void configureLifecycle(User user, Routine routine, ZoneId zoneId) {

        when(userRepository.findAllActiveIds())
                .thenReturn(List.of(user.getId()));

        when(userRepository.findByIdAndIsDeletedFalse(user.getId()))
                .thenReturn(Optional.of(user));

        when(timeZoneResolver.resolve(user))
                .thenReturn(zoneId);

        when(routineRepository
                .findAllByUserIdAndIsDeletedFalseAndSupplementIsDeletedFalse(user.getId()))
                .thenReturn(List.of(routine));

        // Oluşturulacak kayıt yoksa kilit hiç alınmaz; bu yüzden lenient.
        org.mockito.Mockito.lenient().when(routineRepository
                .findActiveByIdAndUserIdForUpdate(routine.getId(), user.getId()))
                .thenReturn(Optional.of(routine));

        when(executionRepository.findExistingDates(
                eq(user.getId()),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of());

        org.mockito.Mockito.lenient().when(executionRepository.findExistingDatesByRoutine(
                eq(routine.getId()),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(Set.of());

        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private Routine routine(Long id, User user, LocalTime time) {
        Routine routine = new Routine();
        routine.setId(id);
        routine.setUser(user);
        routine.setRoutineTime(time);
        return routine;
    }

    private RoutineExecution pending(Routine routine, LocalDate date, LocalTime time) {
        RoutineExecution execution = new RoutineExecution();
        execution.setRoutine(routine);
        execution.setScheduledDate(date);
        execution.setScheduledTime(time);
        execution.setStatus(RoutineExecutionStatus.PENDING);
        return execution;
    }
}
