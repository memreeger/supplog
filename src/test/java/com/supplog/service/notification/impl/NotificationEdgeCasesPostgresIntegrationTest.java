package com.supplog.service.notification.impl;

import com.supplog.dto.notification.DeviceRegisterDto;
import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.dto.notification.NudgeCreateDto;
import com.supplog.dto.user.DeleteUserRequestDto;
import com.supplog.enums.DevicePlatform;
import com.supplog.enums.NotificationType;
import com.supplog.repository.NotificationDispatchLogRepository;
import com.supplog.repository.NotificationPreferenceRepository;
import com.supplog.repository.RoutineExecutionRepository;
import com.supplog.repository.RoutineReminderSettingRepository;
import com.supplog.repository.SupportRelationshipRepository;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.notification.NudgeService;
import com.supplog.service.routine.RoutineService;
import com.supplog.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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

/**
 * Kenar durumlar: rutin/takviye silinince ve hesap dondurulunca bildirimler durmalı, geri alınca bildirim
 * yeniden görünmeli. Zamanlayıcı sabit saatle sürülür.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class NotificationEdgeCasesPostgresIntegrationTest {

    private static final String PASSWORD = "Test-pass-123";
    // Sabit an; doz tarihi gelecekte tutulur ki "bugünden itibaren bekleyenleri sil" kuralı testi tarihe bağımlı olmasın.
    private static final Instant SCHEDULED = Instant.parse("2026-10-23T08:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired NotificationService notificationService;
    @Autowired DeviceTokenService deviceTokenService;
    @Autowired NudgeService nudgeService;
    @Autowired RoutineService routineService;
    @Autowired UserService userService;
    @Autowired RoutineExecutionRepository executionRepository;
    @Autowired RoutineReminderSettingRepository settingRepository;
    @Autowired NotificationPreferenceRepository preferenceRepository;
    @Autowired NotificationDispatchLogRepository dispatchLogRepository;
    @Autowired SupportRelationshipRepository supportRelationshipRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @Test
    void deletingTheRoutineRemovesItsNotificationsAndStopsNewOnes() {
        Scene s = scene("del-routine");
        run(SCHEDULED); // LATE-0: sahibe hatırlatma, destekçiye "vakti geldi"
        assertEquals(1, notificationService.list(s.ownerId, 20).size());
        assertEquals(1, notificationService.list(s.supporterId, 20).size());

        routineService.deleteRoutine(s.ownerId, s.routineId);

        assertTrue(notificationService.list(s.ownerId, 20).isEmpty(), "owner's reminder must disappear");
        assertTrue(notificationService.list(s.supporterId, 20).isEmpty(), "supporter's alert must disappear");
        assertEquals(0, executionCount(s.executionId));

        run(SCHEDULED.plus(Duration.ofMinutes(10))); // sonraki dilim: hiçbir şey üretilmez
        assertEquals(0, notificationService.unreadCount(s.ownerId).getCount());
        assertEquals(0, notificationService.unreadCount(s.supporterId).getCount());
    }

    @Test
    void aDeletedSupplementOrRoutineIsNeverReminded() {
        Scene supplementGone = scene("del-supp");
        jdbc.update("UPDATE supplements SET is_deleted = true WHERE id = ?", supplementGone.supplementId);
        Scene routineGone = scene("del-flag");
        jdbc.update("UPDATE routines SET is_deleted = true WHERE id = ?", routineGone.routineId);

        run(SCHEDULED);

        for (Scene scene : List.of(supplementGone, routineGone)) {
            assertTrue(notificationService.list(scene.ownerId, 20).isEmpty());
            assertTrue(notificationService.list(scene.supporterId, 20).isEmpty());
            assertEquals(0, dispatchRows(scene.executionId));
        }
    }

    @Test
    void freezingTheOwnersAccountStopsReminders_clearsDevicesAndSupporterAlerts() {
        Scene s = scene("freeze-owner");
        deviceTokenService.register(s.ownerId, new DeviceRegisterDto("tok-" + UUID.randomUUID(), DevicePlatform.ANDROID, "tr", "1"));
        run(SCHEDULED.minus(Duration.ofMinutes(30))); // PRE-30: sahibe
        run(SCHEDULED);                               // LATE-0 + destekçiye ON_TIME
        assertEquals(1, notificationService.unreadCount(s.supporterId).getCount());
        long dispatchBefore = dispatchRows(s.executionId);

        userService.deActivateMyProfile(s.ownerId, new DeleteUserRequestDto(PASSWORD));

        assertTrue(deviceTokenService.tokensFor(s.ownerId).isEmpty(), "push devices must be removed");
        assertEquals(0, notificationService.unreadCount(s.supporterId).getCount(), "supporter alert must be resolved");
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT status FROM support_relationships WHERE id = ?", String.class, s.relationshipId));

        run(SCHEDULED.plus(Duration.ofMinutes(10)));
        run(SCHEDULED.plus(Duration.ofMinutes(31))); // kaçırıldı anı da dahil
        assertEquals(dispatchBefore, dispatchRows(s.executionId), "nothing may be processed for a frozen account");
        assertEquals(0, notificationService.unreadCount(s.supporterId).getCount());
    }

    @Test
    void aFrozenSupporterGetsNoAlertsButTheOwnerStillDoes() {
        Scene s = scene("freeze-sup");
        userService.deActivateMyProfile(s.supporterId, new DeleteUserRequestDto(PASSWORD));

        run(SCHEDULED);

        assertTrue(notificationService.list(s.supporterId, 20).isEmpty());
        assertEquals(1, notificationService.unreadCount(s.ownerId).getCount());
    }

    @Test
    void nudgesToOrFromDeletedOrFrozenPeopleAreRejected() {
        Scene routineDeleted = scene("nudge-del");
        jdbc.update("UPDATE routines SET is_deleted = true WHERE id = ?", routineDeleted.routineId);
        assertThrows(RuntimeException.class, () -> nudgeService.send(routineDeleted.supporterId, template(routineDeleted.routineId)));

        Scene ownerFrozen = scene("nudge-owner");
        userService.deActivateMyProfile(ownerFrozen.ownerId, new DeleteUserRequestDto(PASSWORD));
        assertThrows(RuntimeException.class, () -> nudgeService.send(ownerFrozen.supporterId, template(ownerFrozen.routineId)));

        Scene senderFrozen = scene("nudge-sender");
        userService.deActivateMyProfile(senderFrozen.supporterId, new DeleteUserRequestDto(PASSWORD));
        assertThrows(RuntimeException.class, () -> nudgeService.send(senderFrozen.supporterId, template(senderFrozen.routineId)));

        for (Scene scene : List.of(routineDeleted, ownerFrozen, senderFrozen)) {
            assertEquals(0, jdbc.queryForObject(
                    "SELECT count(*) FROM nudges WHERE routine_id = ?", Long.class, scene.routineId));
        }
    }

    @Test
    void completingThenUndoingBringsTheReminderBackOnTheNextSlot() {
        Scene s = scene("undo");
        run(SCHEDULED.minus(Duration.ofMinutes(30)));
        assertEquals(1, notificationService.unreadCount(s.ownerId).getCount());

        // Tamamlandı: çandan düşer. (Doz satırı PENDING bırakılır; geri alınmış doz da böyle görünür.)
        notificationService.resolveForExecution(s.executionId);
        assertEquals(0, notificationService.unreadCount(s.ownerId).getCount());

        // Geri alma sonrası sıradaki dilim satırı yeniden açar.
        run(SCHEDULED.minus(Duration.ofMinutes(20)));
        List<NotificationResponseDto> list = notificationService.list(s.ownerId, 20);
        assertEquals(1, list.size());
        assertEquals(NotificationType.ROUTINE_REMINDER, list.getFirst().getType());
        assertFalse(list.getFirst().isRead());
    }

    private void run(Instant at) {
        new ReminderServiceImpl(executionRepository, settingRepository, preferenceRepository,
                dispatchLogRepository, supportRelationshipRepository, notificationService, transactionTemplate,
                Clock.fixed(at, ZoneOffset.UTC)).processDue();
    }

    private static NudgeCreateDto template(Long routineId) {
        return new NudgeCreateDto(routineId, "TAKE_YOUR_DOSE", null);
    }

    private long dispatchRows(Long executionId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM notification_dispatch_log WHERE execution_id = ?", Long.class, executionId);
    }

    private long executionCount(Long executionId) {
        return jdbc.queryForObject("SELECT count(*) FROM routine_executions WHERE id = ?", Long.class, executionId);
    }

    private record Scene(Long ownerId, Long supporterId, Long supplementId, Long routineId, Long executionId, Long relationshipId) {
    }

    // Kabul edilmiş (ALL_ROUTINES) bir destekçisi olan, 08:00'de dozu bekleyen sahip.
    private Scene scene(String prefix) {
        Long ownerId = newUser(prefix + "-owner");
        Long supporterId = newUser(prefix + "-sup");
        Long supplementId = jdbc.queryForObject("""
                INSERT INTO supplements
                    (created_at, expire_date, is_deleted, supplement_name,
                     supplement_dosage, category, updated_at, inserted_by_user_id)
                VALUES (CURRENT_TIMESTAMP, CURRENT_DATE + 30, false, 'Çinko', '1',
                        'SUPPLEMENT', CURRENT_TIMESTAMP, ?)
                RETURNING id
                """, Long.class, ownerId);
        Long routineId = jdbc.queryForObject("""
                INSERT INTO routines
                    (duration_type, frequency, is_deleted, routine_time, start_date,
                     supplement_id, user_id, created_at, updated_at)
                VALUES ('LIFE_TIME', 'DAILY', false, '08:00', CURRENT_DATE,
                        ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, supplementId, ownerId);
        Long executionId = jdbc.queryForObject("""
                INSERT INTO routine_executions
                    (created_at, scheduled_at, scheduled_date, scheduled_time, scheduled_zone_id,
                     missed_at, status, updated_at, routine_id)
                VALUES (CURRENT_TIMESTAMP, ?, ?, '08:00', 'UTC', ?, 'PENDING', CURRENT_TIMESTAMP, ?)
                RETURNING id
                """, Long.class,
                Timestamp.from(SCHEDULED), LocalDate.now(ZoneOffset.UTC).plusDays(1),
                Timestamp.from(SCHEDULED.plus(Duration.ofMinutes(30))), routineId);
        Long relationshipId = jdbc.queryForObject("""
                INSERT INTO support_relationships
                    (access_scope, created_at, requested_at, status, updated_at,
                     supported_user_id, supporter_user_id)
                VALUES ('ALL_ROUTINES', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACCEPTED', CURRENT_TIMESTAMP, ?, ?)
                RETURNING id
                """, Long.class, ownerId, supporterId);
        return new Scene(ownerId, supporterId, supplementId, routineId, executionId, relationshipId);
    }

    private Long newUser(String prefix) {
        String username = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        return jdbc.queryForObject("""
                INSERT INTO users
                    (birth_date, created_at, e_mail, first_name, is_deleted, last_name,
                     password, score, updated_at, user_name, token_version, time_zone,
                     must_change_password)
                VALUES (?, ?, ?, 'Test', false, 'User', ?, 0, ?, ?, 0, 'UTC', false)
                RETURNING id
                """, Long.class,
                LocalDate.of(1990, 1, 1), LocalDateTime.now(),
                username + "@example.test", passwordEncoder.encode(PASSWORD), LocalDateTime.now(), username);
    }
}
