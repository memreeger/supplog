package com.supplog.service.notification.impl;

import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.enums.NotificationType;
import com.supplog.repository.NotificationDispatchLogRepository;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineReminderSettingRepository;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.service.support.SupportService;
import com.supplog.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class ReminderPostgresIntegrationTest {

    // Sabit bir an: zamanlayıcı gerçek saate değil, test saatine göre çalışır.
    private static final Instant SCHEDULED = Instant.parse("2026-10-23T08:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationService notificationService;
    @Autowired RoutineExecutionRepository executionRepository;
    @Autowired RoutineReminderSettingRepository settingRepository;
    @Autowired NotificationPreferenceRepository preferenceRepository;
    @Autowired NotificationDispatchLogRepository dispatchLogRepository;
    @Autowired SupportRelationshipRepository supportRelationshipRepository;
    @Autowired SupportService supportService;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void reminderIsCreatedOncePerSlotAndUpdatedAcrossSlots() {
        Fixture f = fixture("slots");

        run(SCHEDULED.minus(Duration.ofMinutes(30)));
        run(SCHEDULED.minus(Duration.ofMinutes(30))); // aynı dilim tekrar işlenmez

        List<NotificationResponseDto> list = notificationService.list(f.userId, 20);
        assertEquals(1, list.size());
        assertEquals(NotificationType.ROUTINE_REMINDER, list.getFirst().getType());
        assertEquals("PRE", list.getFirst().getParams().get("phase"));
        assertEquals(30, list.getFirst().getParams().get("minutes"));
        assertEquals("Çinko", list.getFirst().getParams().get("supplementName"));
        assertEquals(1, dispatchRows(f.executionId));
        assertEquals(1, notificationService.unreadCount(f.userId).getCount());

        // Okundu işaretlenir; sonraki dilim yeni bir push'tur, satır tekrar okunmamış olur ve metni güncellenir.
        notificationService.markRead(f.userId, list.getFirst().getId());
        assertEquals(0, notificationService.unreadCount(f.userId).getCount());
        run(SCHEDULED.minus(Duration.ofMinutes(20)));
        assertEquals(1, notificationService.unreadCount(f.userId).getCount());
        assertEquals(20, notificationService.list(f.userId, 20).getFirst().getParams().get("minutes"));

        // Faz PRE → LATE olunca da hâlâ tek satırdır ve okunmamıştır.
        notificationService.markRead(f.userId, notificationService.list(f.userId, 20).getFirst().getId());
        run(SCHEDULED);
        List<NotificationResponseDto> late = notificationService.list(f.userId, 20);
        assertEquals(1, late.size());
        assertEquals("LATE", late.getFirst().getParams().get("phase"));
        assertEquals(30, late.getFirst().getParams().get("minutes"));
        assertEquals(1, notificationService.unreadCount(f.userId).getCount());
    }

    @Test
    void completingTheDoseRemovesTheReminderFromTheBell() {
        Fixture f = fixture("complete");
        run(SCHEDULED.minus(Duration.ofMinutes(10)));
        assertEquals(1, notificationService.unreadCount(f.userId).getCount());

        jdbc.update("UPDATE routine_executions SET status = 'COMPLETED' WHERE id = ?", f.executionId);
        notificationService.resolveForExecution(f.executionId);

        assertEquals(0, notificationService.unreadCount(f.userId).getCount());
        assertTrue(notificationService.list(f.userId, 20).isEmpty());

        // Tamamlanmış doz artık aday değildir: sonraki dilimler hiçbir şey yazmaz.
        run(SCHEDULED.minus(Duration.ofMinutes(5)));
        assertTrue(notificationService.list(f.userId, 20).isEmpty());
    }

    @Test
    void missedDoseReplacesTheReminderWithAMissedNotification() {
        Fixture f = fixture("missed");
        run(SCHEDULED.plus(Duration.ofMinutes(10)));
        assertEquals("LATE", notificationService.list(f.userId, 20).getFirst().getParams().get("phase"));

        jdbc.update("UPDATE routine_executions SET status = 'MISSED' WHERE id = ?", f.executionId);
        run(SCHEDULED.plus(Duration.ofMinutes(31)));
        run(SCHEDULED.plus(Duration.ofMinutes(32))); // ikinci tur tekrar üretmez

        List<NotificationResponseDto> list = notificationService.list(f.userId, 20);
        assertEquals(1, list.size());
        assertEquals(NotificationType.ROUTINE_MISSED, list.getFirst().getType());
        assertEquals("Çinko", list.getFirst().getParams().get("supplementName"));
        assertEquals(1, notificationService.unreadCount(f.userId).getCount());
    }

    @Test
    void staleSlotsAreSkippedAfterDowntime() {
        Fixture f = fixture("stale");

        // PRE-30 (07:30) 4 dakika önce kaldı, sıradaki dilim (07:40) henüz yok: gönderilmez.
        run(SCHEDULED.minus(Duration.ofMinutes(26)));

        assertTrue(notificationService.list(f.userId, 20).isEmpty());
        assertEquals(0, dispatchRows(f.executionId));
    }

    @Test
    void routineSettingAndUserPreferenceCanSilenceReminders() {
        Fixture disabledRoutine = fixture("off-routine");
        jdbc.update("""
                INSERT INTO routine_reminder_settings (routine_id, enabled, lead_minutes, created_at, updated_at)
                VALUES (?, false, 30, now(), now())
                """, disabledRoutine.routineId);

        Fixture optedOut = fixture("off-user");
        jdbc.update("""
                INSERT INTO notification_preferences
                    (user_id, push_enabled, reminders_enabled, supporter_alerts_enabled, nudges_enabled,
                     hide_details_in_push, created_at, updated_at)
                VALUES (?, true, false, true, true, false, now(), now())
                """, optedOut.userId);

        run(SCHEDULED.minus(Duration.ofMinutes(30)));

        assertTrue(notificationService.list(disabledRoutine.userId, 20).isEmpty());
        assertTrue(notificationService.list(optedOut.userId, 20).isEmpty());
    }

    @Test
    void leadMinutesSettingMovesTheFirstReminder() {
        Fixture f = fixture("lead");
        jdbc.update("""
                INSERT INTO routine_reminder_settings (routine_id, enabled, lead_minutes, created_at, updated_at)
                VALUES (?, true, 10, now(), now())
                """, f.routineId);

        run(SCHEDULED.minus(Duration.ofMinutes(30))); // 10 dk lead'de 30 dk önce hiçbir dilim yok
        assertTrue(notificationService.list(f.userId, 20).isEmpty());

        run(SCHEDULED.minus(Duration.ofMinutes(10)));
        assertEquals(10, notificationService.list(f.userId, 20).getFirst().getParams().get("minutes"));
    }

    @Test
    void supportersOnlyGetAlertsForRoutinesTheyMaySee() {
        Fixture owner = fixture("sup-owner");
        Fixture otherRoutineOwner = fixture("sup-unrelated");
        Long all = supporter(owner, "ALL_ROUTINES", "ACCEPTED");
        Long selectedWithAccess = supporter(owner, "SELECTED_ROUTINES", "ACCEPTED", owner.routineId);
        Long selectedWithoutAccess = supporter(owner, "SELECTED_ROUTINES", "ACCEPTED", otherRoutineOwner.routineId);
        Long pendingOnly = supporter(owner, "ALL_ROUTINES", "PENDING");
        Long revoked = supporter(owner, "ALL_ROUTINES", "REVOKED");

        run(SCHEDULED);

        assertEquals(1, notificationService.unreadCount(all).getCount());
        assertEquals(1, notificationService.unreadCount(selectedWithAccess).getCount());
        assertEquals(0, notificationService.unreadCount(selectedWithoutAccess).getCount());
        assertEquals(0, notificationService.unreadCount(pendingOnly).getCount());
        assertEquals(0, notificationService.unreadCount(revoked).getCount());

        NotificationResponseDto alert = notificationService.list(all, 20).getFirst();
        assertEquals(NotificationType.SUPPORT_ALERT, alert.getType());
        assertEquals("ON_TIME", alert.getParams().get("phase"));
        assertEquals("Test", alert.getParams().get("ownerName"));
        assertEquals("Çinko", alert.getParams().get("supplementName"));
        assertEquals(owner.userId, alert.getActorUserId());
    }

    @Test
    void supporterAlertGoesThroughOnTimeLastCallAndMissed() {
        Fixture owner = fixture("sup-stages");
        Long supporterId = supporter(owner, "ALL_ROUTINES", "ACCEPTED");

        run(SCHEDULED);
        run(SCHEDULED); // aynı dilim tekrar işlenmez
        assertEquals(1, notificationService.list(supporterId, 20).size());
        notificationService.markRead(supporterId, notificationService.list(supporterId, 20).getFirst().getId());
        assertEquals(0, notificationService.unreadCount(supporterId).getCount());

        run(SCHEDULED.plus(Duration.ofMinutes(25)));
        NotificationResponseDto lastCall = notificationService.list(supporterId, 20).getFirst();
        assertEquals("LAST_CALL", lastCall.getParams().get("phase"));
        assertEquals(5, lastCall.getParams().get("minutes"));
        assertEquals(1, notificationService.unreadCount(supporterId).getCount()); // aşama değişti → yeniden okunmamış

        jdbc.update("UPDATE routine_executions SET status = 'MISSED' WHERE id = ?", owner.executionId);
        run(SCHEDULED.plus(Duration.ofMinutes(31)));
        List<NotificationResponseDto> list = notificationService.list(supporterId, 20);
        assertEquals(1, list.size());
        assertEquals("MISSED", list.getFirst().getParams().get("phase"));
        assertEquals(1, notificationService.unreadCount(supporterId).getCount());
        // Sahibin kendi "kaçırdın" bildirimi de ayrıca duruyor.
        assertEquals(NotificationType.ROUTINE_MISSED, notificationService.list(owner.userId, 20).getFirst().getType());
    }

    @Test
    void completingTheDoseClearsTheSupporterAlertToo() {
        Fixture owner = fixture("sup-complete");
        Long supporterId = supporter(owner, "ALL_ROUTINES", "ACCEPTED");
        run(SCHEDULED);
        assertEquals(1, notificationService.unreadCount(supporterId).getCount());

        jdbc.update("UPDATE routine_executions SET status = 'COMPLETED' WHERE id = ?", owner.executionId);
        notificationService.resolveForExecution(owner.executionId);

        assertEquals(0, notificationService.unreadCount(supporterId).getCount());
    }

    @Test
    void supporterCanTurnAlertsOff() {
        Fixture owner = fixture("sup-pref");
        Long supporterId = supporter(owner, "ALL_ROUTINES", "ACCEPTED");
        jdbc.update("""
                INSERT INTO notification_preferences
                    (user_id, push_enabled, reminders_enabled, supporter_alerts_enabled, nudges_enabled,
                     hide_details_in_push, created_at, updated_at)
                VALUES (?, true, true, false, true, false, now(), now())
                """, supporterId);

        run(SCHEDULED);

        assertTrue(notificationService.list(supporterId, 20).isEmpty());
        assertEquals(1, notificationService.unreadCount(owner.userId).getCount()); // sahibin hatırlatması etkilenmez
    }

    @Test
    void revokingOrNarrowingAccessRemovesTheSupportersAlerts() {
        Fixture owner = fixture("sup-access");
        Long revokedSupporter = supporter(owner, "ALL_ROUTINES", "ACCEPTED");
        Long narrowedSupporter = supporter(owner, "ALL_ROUTINES", "ACCEPTED");
        run(SCHEDULED);
        assertEquals(1, notificationService.unreadCount(revokedSupporter).getCount());
        assertEquals(1, notificationService.unreadCount(narrowedSupporter).getCount());

        supportService.revokeRelationship(owner.userId, relationshipOf(revokedSupporter));
        com.supplog.dto.support.SupportAccessUpdateDto narrow = new com.supplog.dto.support.SupportAccessUpdateDto();
        narrow.setAccessScope(com.supplog.enums.SupportAccessScope.SELECTED_ROUTINES);
        supportService.updateAccessScope(owner.userId, relationshipOf(narrowedSupporter), narrow);

        assertEquals(0, notificationService.unreadCount(revokedSupporter).getCount());
        assertEquals(0, notificationService.unreadCount(narrowedSupporter).getCount());
        assertEquals(1, notificationService.unreadCount(owner.userId).getCount()); // sahibin hatırlatması kalır
    }

    private void run(Instant at) {
        new ReminderServiceImpl(executionRepository, settingRepository, preferenceRepository,
                dispatchLogRepository, supportRelationshipRepository, notificationService, transactionTemplate,
                Clock.fixed(at, ZoneOffset.UTC)).processDue();
    }

    private long dispatchRows(Long executionId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM notification_dispatch_log WHERE execution_id = ?", Long.class, executionId);
    }

    private record Fixture(Long userId, Long routineId, Long executionId) {
    }

    private Long newUser(String prefix) {
        String username = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbc.queryForObject("""
                INSERT INTO users
                    (birth_date, created_at, e_mail, first_name, is_deleted, last_name,
                     password, score, updated_at, user_name, token_version, time_zone,
                     must_change_password)
                VALUES (?, ?, ?, 'Test', false, 'User', 'x', 0, ?, ?, 0, 'UTC', false)
                RETURNING id
                """, Long.class,
                LocalDate.of(1990, 1, 1), LocalDateTime.now(),
                username + "@example.test", LocalDateTime.now(), username);
    }

    // Sahibin rutinini paylaşan bir destekçi; permittedRoutineIds yalnızca SELECTED_ROUTINES için anlamlı.
    private Long supporter(Fixture owner, String scope, String status, Long... permittedRoutineIds) {
        Long supporterId = newUser("supporter");
        Long relationshipId = jdbc.queryForObject("""
                INSERT INTO support_relationships
                    (access_scope, created_at, requested_at, status, updated_at,
                     supported_user_id, supporter_user_id)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?, ?)
                RETURNING id
                """, Long.class, scope, status, owner.userId, supporterId);
        for (Long routineId : permittedRoutineIds) {
            jdbc.update("""
                    INSERT INTO support_routine_permissions (created_at, routine_id, support_relationship_id)
                    VALUES (CURRENT_TIMESTAMP, ?, ?)
                    """, routineId, relationshipId);
        }
        return supporterId;
    }

    private Long relationshipOf(Long supporterId) {
        return jdbc.queryForObject(
                "SELECT id FROM support_relationships WHERE supporter_user_id = ?", Long.class, supporterId);
    }

    private Fixture fixture(String prefix) {
        Long userId = newUser(prefix);
        Long supplementId = jdbc.queryForObject("""
                INSERT INTO supplements
                    (created_at, expire_date, is_deleted, supplement_name,
                     supplement_dosage, category, updated_at, inserted_by_user_id)
                VALUES (CURRENT_TIMESTAMP, CURRENT_DATE + 30, false, 'Çinko', '1',
                        'SUPPLEMENT', CURRENT_TIMESTAMP, ?)
                RETURNING id
                """, Long.class, userId);
        Long routineId = jdbc.queryForObject("""
                INSERT INTO routines
                    (duration_type, frequency, is_deleted, routine_time, start_date,
                     supplement_id, user_id, created_at, updated_at)
                VALUES ('LIFE_TIME', 'DAILY', false, '08:00', CURRENT_DATE,
                        ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, supplementId, userId);
        Long executionId = jdbc.queryForObject("""
                INSERT INTO routine_executions
                    (created_at, scheduled_at, scheduled_date, scheduled_time, scheduled_zone_id,
                     missed_at, status, updated_at, routine_id)
                VALUES (CURRENT_TIMESTAMP, ?, ?, '08:00', 'UTC', ?, 'PENDING', CURRENT_TIMESTAMP, ?)
                RETURNING id
                """, Long.class,
                Timestamp.from(SCHEDULED), LocalDate.of(2026, 10, 23),
                Timestamp.from(SCHEDULED.plus(Duration.ofMinutes(30))), routineId);
        return new Fixture(userId, routineId, executionId);
    }
}
