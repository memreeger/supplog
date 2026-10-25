package com.supplog;

import com.supplog.dto.notification.NotificationPreferenceDto;
import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.service.notification.NotificationPreferenceService;
import com.supplog.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class NotificationPostgresIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationService notificationService;
    @Autowired NotificationPreferenceService preferenceService;

    @Test
    void listCountAndMarkReadFollowTheInboxRules() {
        Long userId = insertUser("inbox");
        Long first = insertNotification(userId, "k1", Instant.now().minus(2, ChronoUnit.MINUTES), false);
        Long second = insertNotification(userId, "k2", Instant.now().minus(1, ChronoUnit.MINUTES), false);
        insertNotification(userId, "resolved", Instant.now(), true);

        List<NotificationResponseDto> list = notificationService.list(userId, 20);

        // Çözülmüş satır görünmez, en yeni önce gelir; params JSONB'den geri okunur.
        assertEquals(List.of(second, first), list.stream().map(NotificationResponseDto::getId).toList());
        assertEquals("Çinko", list.get(0).getParams().get("supplementName"));
        assertEquals(2, notificationService.unreadCount(userId).getCount());

        notificationService.markRead(userId, second);
        notificationService.markRead(userId, second); // idempotent

        assertEquals(1, notificationService.unreadCount(userId).getCount());
        assertTrue(notificationService.list(userId, 20).get(0).isRead());

        notificationService.markAllRead(userId);
        assertEquals(0, notificationService.unreadCount(userId).getCount());
    }

    @Test
    void cannotReadAnotherUsersNotification() {
        Long owner = insertUser("owner");
        Long stranger = insertUser("stranger");
        Long notificationId = insertNotification(owner, "private", Instant.now(), false);

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markRead(stranger, notificationId));
        assertEquals(1, notificationService.unreadCount(owner).getCount());
        assertEquals(0, notificationService.unreadCount(stranger).getCount());
        assertTrue(notificationService.list(stranger, 20).isEmpty());
    }

    @Test
    void markAllReadOnlyTouchesTheCallersRows() {
        Long a = insertUser("all-a");
        Long b = insertUser("all-b");
        insertNotification(a, "a1", Instant.now(), false);
        insertNotification(b, "b1", Instant.now(), false);

        notificationService.markAllRead(a);

        assertEquals(0, notificationService.unreadCount(a).getCount());
        assertEquals(1, notificationService.unreadCount(b).getCount());
    }

    @Test
    void cleanupDeletesOnlyOldNotifications() {
        Long userId = insertUser("cleanup");
        Long old = insertNotification(userId, "old", Instant.now().minus(120, ChronoUnit.DAYS), false);
        Long fresh = insertNotification(userId, "fresh", Instant.now(), false);

        notificationService.deleteOldNotifications();

        assertEquals(0, count(old));
        assertEquals(1, count(fresh));
    }

    @Test
    void preferencesDefaultToEnabledAndPersistUpdates() {
        Long userId = insertUser("prefs");

        NotificationPreferenceDto defaults = preferenceService.get(userId);
        assertTrue(defaults.getPushEnabled());
        assertTrue(defaults.getNudgesEnabled());
        assertFalse(defaults.getHideDetailsInPush());

        preferenceService.update(userId, new NotificationPreferenceDto(true, false, true, false, true));

        NotificationPreferenceDto saved = preferenceService.get(userId);
        assertFalse(saved.getRemindersEnabled());
        assertFalse(saved.getNudgesEnabled());
        assertTrue(saved.getHideDetailsInPush());
    }

    private long count(Long notificationId) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE id = ?", Long.class, notificationId);
    }

    private Long insertNotification(Long userId, String dedupeKey, Instant createdAt, boolean resolved) {
        Timestamp created = Timestamp.from(createdAt);
        return jdbc.queryForObject("""
                INSERT INTO notifications
                    (recipient_user_id, type, dedupe_key, params, resolved_at, created_at, updated_at)
                VALUES (?, 'ROUTINE_REMINDER', ?, ?::jsonb, ?, ?, ?)
                RETURNING id
                """, Long.class, userId, dedupeKey,
                "{\"supplementName\":\"Çinko\"}",
                resolved ? created : null, created, created);
    }

    private Long insertUser(String prefix) {
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
