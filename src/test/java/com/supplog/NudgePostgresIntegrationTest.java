package com.supplog;

import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.dto.notification.NudgeCreateDto;
import com.supplog.enums.NotificationType;
import com.supplog.exception.BusinessException;
import com.supplog.exception.RateLimitedException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.notification.NudgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class NudgePostgresIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired NudgeService nudgeService;
    @Autowired NotificationService notificationService;

    @Test
    void supporterNudgesTheOwnerWithATemplateAndTheOwnerSeesItInTheBell() {
        Scene scene = scene("ALL_ROUTINES", "ACCEPTED");

        nudgeService.send(scene.supporterId, template(scene.routineId, "TAKE_YOUR_DOSE"));

        List<NotificationResponseDto> inbox = notificationService.list(scene.ownerId, 20);
        assertEquals(1, inbox.size());
        NotificationResponseDto nudge = inbox.getFirst();
        assertEquals(NotificationType.NUDGE, nudge.getType());
        assertEquals("TAKE_YOUR_DOSE", nudge.getParams().get("templateKey"));
        assertEquals("Çinko", nudge.getParams().get("supplementName"));
        assertEquals("Test", nudge.getParams().get("senderName"));
        assertEquals(scene.supporterId, nudge.getActorUserId());
        assertNotNull(nudge.getNudgeId());
        assertEquals(1, notificationService.unreadCount(scene.ownerId).getCount());
        assertEquals(0, notificationService.unreadCount(scene.supporterId).getCount());
    }

    @Test
    void freeTextIsCleanedAndStoredAsASnapshot() {
        Scene scene = scene("ALL_ROUTINES", "ACCEPTED");

        nudgeService.send(scene.supporterId, text(scene.routineId, "  bro \n rutinini   almadın​  "));

        NotificationResponseDto nudge = notificationService.list(scene.ownerId, 20).getFirst();
        assertEquals("bro rutinini almadın", nudge.getParams().get("message"));
        assertNull(nudge.getParams().get("templateKey"));
        assertEquals("bro rutinini almadın",
                jdbc.queryForObject("SELECT message FROM nudges WHERE id = ?", String.class, nudge.getNudgeId()));
    }

    @Test
    void contentMustBeExactlyOneOfTemplateOrText() {
        Scene scene = scene("ALL_ROUTINES", "ACCEPTED");

        assertBusiness("nudge.content.invalid", () -> nudgeService.send(scene.supporterId,
                new NudgeCreateDto(scene.routineId, null, null)));
        assertBusiness("nudge.content.invalid", () -> nudgeService.send(scene.supporterId,
                new NudgeCreateDto(scene.routineId, "TAKE_YOUR_DOSE", "ve metin")));
        assertBusiness("nudge.template.invalid", () -> nudgeService.send(scene.supporterId, template(scene.routineId, "NOPE")));
        assertBusiness("nudge.message.invalid", () -> nudgeService.send(scene.supporterId, text(scene.routineId, "bak evil.com")));
        assertBusiness("nudge.message.invalid", () -> nudgeService.send(scene.supporterId, text(scene.routineId, "a".repeat(281))));
        assertEquals(0, notificationService.unreadCount(scene.ownerId).getCount());
    }

    @Test
    void onlyAcceptedSupportersOfAPermittedRoutineMaySend() {
        Scene pending = scene("ALL_ROUTINES", "PENDING");
        Scene revoked = scene("ALL_ROUTINES", "REVOKED");
        Scene selectedWithoutAccess = scene("SELECTED_ROUTINES", "ACCEPTED");
        Scene stranger = scene("ALL_ROUTINES", "ACCEPTED");
        Long outsider = newUser("outsider");

        for (Scene scene : List.of(pending, revoked, selectedWithoutAccess)) {
            assertThrows(ResourceNotFoundException.class,
                    () -> nudgeService.send(scene.supporterId, template(scene.routineId, "DONT_FORGET")));
        }
        assertThrows(ResourceNotFoundException.class,
                () -> nudgeService.send(outsider, template(stranger.routineId, "DONT_FORGET")));
        assertThrows(ResourceNotFoundException.class,
                () -> nudgeService.send(stranger.supporterId, template(-5L, "DONT_FORGET")));
        // Sahibin kendi rutinine ya da yön tersine gönderim yok.
        assertBusiness("nudge.self.not.allowed", () -> nudgeService.send(stranger.ownerId, template(stranger.routineId, "DONT_FORGET")));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM nudges WHERE recipient_user_id = ?", Long.class, pending.ownerId));
    }

    @Test
    void selectedScopeWithPermissionIsAllowed() {
        Scene scene = scene("SELECTED_ROUTINES", "ACCEPTED");
        jdbc.update("""
                INSERT INTO support_routine_permissions (created_at, routine_id, support_relationship_id)
                VALUES (CURRENT_TIMESTAMP, ?, ?)
                """, scene.routineId, scene.relationshipId);

        nudgeService.send(scene.supporterId, template(scene.routineId, "YOU_GOT_THIS"));

        assertEquals(1, notificationService.unreadCount(scene.ownerId).getCount());
    }

    @Test
    void recipientCanTurnNudgesOffOrMuteOneSupporter() {
        Scene off = scene("ALL_ROUTINES", "ACCEPTED");
        jdbc.update("""
                INSERT INTO notification_preferences
                    (user_id, push_enabled, reminders_enabled, supporter_alerts_enabled, nudges_enabled,
                     hide_details_in_push, created_at, updated_at)
                VALUES (?, true, true, true, false, false, now(), now())
                """, off.ownerId);
        assertBusiness("nudge.disabled.by.recipient", () -> nudgeService.send(off.supporterId, template(off.routineId, "STILL_WAITING")));

        Scene muted = scene("ALL_ROUTINES", "ACCEPTED");
        nudgeService.mute(muted.ownerId, muted.supporterId);
        nudgeService.mute(muted.ownerId, muted.supporterId); // idempotent
        assertEquals(1, nudgeService.listMutes(muted.ownerId).size());
        assertEquals(muted.supporterId, nudgeService.listMutes(muted.ownerId).getFirst().getSenderUserId());
        assertBusiness("nudge.disabled.by.recipient", () -> nudgeService.send(muted.supporterId, template(muted.routineId, "STILL_WAITING")));

        nudgeService.unmute(muted.ownerId, muted.supporterId);
        assertTrue(nudgeService.listMutes(muted.ownerId).isEmpty());
        nudgeService.send(muted.supporterId, template(muted.routineId, "STILL_WAITING"));
        assertEquals(1, notificationService.unreadCount(muted.ownerId).getCount());
    }

    @Test
    void rateLimitsApplyPerRoutineAndPerPair() {
        Scene scene = scene("ALL_ROUTINES", "ACCEPTED");
        nudgeService.send(scene.supporterId, template(scene.routineId, "TAKE_YOUR_DOSE"));

        // Aynı rutine 10 dk'da ikinci dürtme olmaz.
        assertThrows(RateLimitedException.class,
                () -> nudgeService.send(scene.supporterId, template(scene.routineId, "DONT_FORGET")));

        // Saatte 5: önceki dürtmeleri 20 dk öncesine çekip (rutin soğuma süresi geçti) beşe tamamla.
        jdbc.update("UPDATE nudges SET created_at = now() - interval '20 minutes' WHERE sender_user_id = ?", scene.supporterId);
        for (int i = 0; i < 4; i++) {
            jdbc.update("""
                    INSERT INTO nudges (sender_user_id, recipient_user_id, routine_id, template_key, created_at)
                    VALUES (?, ?, ?, 'DONT_FORGET', now() - interval '30 minutes')
                    """, scene.supporterId, scene.ownerId, scene.routineId);
        }
        assertThrows(RateLimitedException.class,
                () -> nudgeService.send(scene.supporterId, template(scene.routineId, "DONT_FORGET")));

        // Başka bir gönderen etkilenmez; sınır gönderen başınadır.
        Scene other = scene("ALL_ROUTINES", "ACCEPTED");
        nudgeService.send(other.supporterId, template(other.routineId, "DONT_FORGET"));
    }

    @Test
    void dailyLimitStopsTheTwentyFirstNudge() {
        Scene scene = scene("ALL_ROUTINES", "ACCEPTED");
        for (int i = 0; i < 20; i++) {
            // Saatlik ve rutin sınırlarına takılmadan günlüğe ulaşmak için 2-23 saat öncesine yay.
            jdbc.update("""
                    INSERT INTO nudges (sender_user_id, recipient_user_id, routine_id, template_key, created_at)
                    VALUES (?, ?, ?, 'DONT_FORGET', now() - (? * interval '1 hour'))
                    """, scene.supporterId, scene.ownerId, scene.routineId, 2 + (i % 20));
        }

        assertThrows(RateLimitedException.class,
                () -> nudgeService.send(scene.supporterId, template(scene.routineId, "DONT_FORGET")));
    }

    private static void assertBusiness(String key, org.junit.jupiter.api.function.Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);
        assertEquals(key, e.getMessage());
    }

    private static NudgeCreateDto template(Long routineId, String key) {
        return new NudgeCreateDto(routineId, key, null);
    }

    private static NudgeCreateDto text(Long routineId, String message) {
        return new NudgeCreateDto(routineId, null, message);
    }

    private record Scene(Long ownerId, Long supporterId, Long routineId, Long relationshipId) {
    }

    private Scene scene(String scope, String status) {
        Long ownerId = newUser("owner");
        Long supporterId = newUser("supporter");
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
        Long relationshipId = jdbc.queryForObject("""
                INSERT INTO support_relationships
                    (access_scope, created_at, requested_at, status, updated_at,
                     supported_user_id, supporter_user_id)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?, ?)
                RETURNING id
                """, Long.class, scope, status, ownerId, supporterId);
        return new Scene(ownerId, supporterId, routineId, relationshipId);
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
}
