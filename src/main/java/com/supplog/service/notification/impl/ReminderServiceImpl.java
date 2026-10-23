package com.supplog.service.notification.impl;

import com.supplog.entity.NotificationPreference;
import com.supplog.entity.RoutineExecution;
import com.supplog.entity.RoutineReminderSetting;
import com.supplog.enums.NotificationType;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.repository.NotificationDispatchLogRepository;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineReminderSettingRepository;
import com.supplog.service.notification.NotificationDraft;
import com.supplog.service.notification.NotificationParams;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.notification.ReminderPhase;
import com.supplog.service.notification.ReminderService;
import com.supplog.service.notification.ReminderSlot;
import com.supplog.service.notification.ReminderSlots;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReminderServiceImpl implements ReminderService {

    // En uzun hatırlatma penceresi (lead 60 dk): bundan uzaktaki dozlar aday bile olmaz.
    private static final Duration MAX_LEAD = Duration.ofMinutes(60);
    // Kaçırıldı bildirimi için geriye bakış; uygulama uzun kapalı kaldıysa eski olayları göndermeyiz.
    private static final Duration MISSED_LOOKBACK = Duration.ofHours(1);
    private static final Duration LOG_RETENTION = Duration.ofDays(7);

    private final RoutineExecutionRepository executionRepository;
    private final RoutineReminderSettingRepository settingRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationDispatchLogRepository dispatchLogRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Autowired
    public ReminderServiceImpl(
            RoutineExecutionRepository executionRepository,
            RoutineReminderSettingRepository settingRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationDispatchLogRepository dispatchLogRepository,
            NotificationService notificationService,
            TransactionTemplate transactionTemplate
    ) {
        this(executionRepository, settingRepository, preferenceRepository, dispatchLogRepository,
                notificationService, transactionTemplate, Clock.systemUTC());
    }

    // Testlerde saat enjekte edilebilsin diye.
    ReminderServiceImpl(
            RoutineExecutionRepository executionRepository,
            RoutineReminderSettingRepository settingRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationDispatchLogRepository dispatchLogRepository,
            NotificationService notificationService,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.executionRepository = executionRepository;
        this.settingRepository = settingRepository;
        this.preferenceRepository = preferenceRepository;
        this.dispatchLogRepository = dispatchLogRepository;
        this.notificationService = notificationService;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public void processDue() {
        Instant now = clock.instant();
        processReminders(now);
        processMissed(now);
    }

    @Override
    @Scheduled(cron = "${app.notifications.dispatch-log-cleanup-cron:0 30 4 * * *}")
    public void deleteOldDispatchLog() {
        int deleted = transactionTemplate.execute(status ->
                dispatchLogRepository.deleteSentBefore(clock.instant().minus(LOG_RETENTION)));
        log.info("Dispatch log cleanup finished, deleted={}", deleted);
    }

    private void processReminders(Instant now) {
        List<RoutineExecution> candidates = executionRepository.findReminderCandidates(
                RoutineExecutionStatus.PENDING, now, now.plus(MAX_LEAD));
        if (candidates.isEmpty()) {
            return;
        }

        Lookups lookups = lookups(candidates);
        for (RoutineExecution execution : candidates) {
            try {
                transactionTemplate.executeWithoutResult(status -> remind(execution, lookups, now));
            } catch (Exception e) {
                // Bir doz patlasa da diğerleri işlenmeye devam eder.
                log.error("Reminder failed for executionId={}", execution.getId(), e);
            }
        }
    }

    private void remind(RoutineExecution candidate, Lookups lookups, Instant now) {
        if (!lookups.remindersAllowed(candidate)) {
            return;
        }

        int lead = lookups.setting(candidate).getLeadMinutes();
        ReminderSlot slot = ReminderSlots
                .currentSlot(ReminderSlots.slotsFor(candidate.getScheduledAt(), candidate.getMissedAt(), lead), now)
                .orElse(null);
        if (slot == null) {
            return;
        }

        // Aday listesi alındıktan sonra kullanıcı dozu tamamlamış olabilir: kilitleyip durumu tekrar bakarız.
        // Tamamlama da aynı satırı kilitler, bu yüzden bayat bir hatırlatma çana yazılmaz.
        RoutineExecution current = executionRepository.findByIdForUpdate(candidate.getId()).orElse(null);
        if (current == null || current.getStatus() != RoutineExecutionStatus.PENDING) {
            return;
        }

        Long userId = candidate.getRoutine().getUser().getId();
        if (dispatchLogRepository.claim(candidate.getId(), userId, slot.kind(), slot.code(), now) == 0) {
            return;
        }

        notificationService.upsertForExecution(new NotificationDraft(
                userId,
                NotificationType.ROUTINE_REMINDER,
                "exec:" + candidate.getId() + ":REMINDER",
                candidate.getId(),
                candidate.getRoutine().getId(),
                null,
                NotificationParams.of(
                        "supplementName", candidate.getRoutine().getSupplement().getName(),
                        "phase", slot.phase().name(),
                        "minutes", slot.minutes(),
                        "scheduledAt", candidate.getScheduledAt(),
                        "missedAt", candidate.getMissedAt()
                )
        ));
    }

    private void processMissed(Instant now) {
        List<RoutineExecution> missed = executionRepository.findRecentlyMissed(
                RoutineExecutionStatus.MISSED, now.minus(MISSED_LOOKBACK), now);
        if (missed.isEmpty()) {
            return;
        }

        Lookups lookups = lookups(missed);
        for (RoutineExecution execution : missed) {
            try {
                transactionTemplate.executeWithoutResult(status -> notifyMissed(execution, lookups, now));
            } catch (Exception e) {
                log.error("Missed notification failed for executionId={}", execution.getId(), e);
            }
        }
    }

    private void notifyMissed(RoutineExecution execution, Lookups lookups, Instant now) {
        if (!lookups.remindersAllowed(execution)) {
            return;
        }

        ReminderSlot slot = ReminderSlots.missedSlot(execution.getMissedAt());
        Long userId = execution.getRoutine().getUser().getId();
        if (dispatchLogRepository.claim(execution.getId(), userId, slot.kind(), slot.code(), now) == 0) {
            return;
        }

        // Önce hatırlatmayı çöz, sonra "kaçırdın" satırını yaz (sıra ters olursa yeni satır da çözülürdü).
        notificationService.resolveForExecution(execution.getId());
        notificationService.upsertForExecution(new NotificationDraft(
                userId,
                NotificationType.ROUTINE_MISSED,
                "exec:" + execution.getId() + ":MISSED",
                execution.getId(),
                execution.getRoutine().getId(),
                null,
                NotificationParams.of(
                        "supplementName", execution.getRoutine().getSupplement().getName(),
                        "phase", ReminderPhase.MISSED.name(),
                        "missedAt", execution.getMissedAt()
                )
        ));
    }

    // Ayar ve tercihleri tur başına iki sorguyla toplu okuruz (doz başına sorgu yok).
    private Lookups lookups(List<RoutineExecution> executions) {
        Set<Long> routineIds = executions.stream().map(e -> e.getRoutine().getId()).collect(Collectors.toSet());
        Set<Long> userIds = executions.stream().map(e -> e.getRoutine().getUser().getId()).collect(Collectors.toSet());
        return new Lookups(
                settingRepository.findAllById(routineIds).stream()
                        .collect(Collectors.toMap(RoutineReminderSetting::getRoutineId, Function.identity())),
                preferenceRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity()))
        );
    }

    private record Lookups(
            Map<Long, RoutineReminderSetting> settings,
            Map<Long, NotificationPreference> preferences
    ) {
        RoutineReminderSetting setting(RoutineExecution execution) {
            Long routineId = execution.getRoutine().getId();
            return settings.getOrDefault(routineId, new RoutineReminderSetting(routineId));
        }

        // Rutin ayarı da kullanıcı tercihi de varsayılan olarak açıktır.
        boolean remindersAllowed(RoutineExecution execution) {
            NotificationPreference preference = preferences.get(execution.getRoutine().getUser().getId());
            return setting(execution).isEnabled() && (preference == null || preference.isRemindersEnabled());
        }
    }
}
