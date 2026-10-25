package com.supplog;

import com.supplog.dto.notification.DeviceRegisterDto;
import com.supplog.enums.DevicePlatform;
import com.supplog.enums.NotificationType;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.notification.NotificationDraft;
import com.supplog.service.notification.NotificationParams;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.notification.push.PushOutcome;
import com.supplog.service.notification.push.PushPayload;
import com.supplog.service.notification.push.PushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Push yalnızca commit sonrası, ayrı thread'de gider; rollback'te hiç gitmez. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
@Import(PushPostgresIntegrationTest.RecordingConfig.class)
class PushPostgresIntegrationTest {

    record Sent(String token, PushPayload payload) {
    }

    static final List<Sent> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class RecordingConfig {
        @Bean
        @Primary
        PushSender recordingPushSender() {
            return (token, payload) -> {
                SENT.add(new Sent(token, payload));
                return PushOutcome.SENT;
            };
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationService notificationService;
    @Autowired DeviceTokenService deviceTokenService;
    @Autowired TransactionTemplate transactionTemplate;

    @BeforeEach
    void clear() {
        SENT.clear();
    }

    @Test
    void pushGoesToTheUsersDevicesAfterCommit() throws Exception {
        Long userId = newUser("push");
        String token = "push-token-" + UUID.randomUUID();
        deviceTokenService.register(userId, new DeviceRegisterDto(token, DevicePlatform.ANDROID, "tr", "1.0"));

        notificationService.upsertForExecution(draft(userId, "exec:1:REMINDER"));

        Sent sent = awaitSent(token);
        assertEquals("Çinko vakti yaklaşıyor", sent.payload().title());
        assertEquals("exec:1:REMINDER", sent.payload().tag());
        assertEquals("ROUTINE_REMINDER", sent.payload().data().get("type"));
        assertEquals(1, notificationService.unreadCount(userId).getCount()); // inbox satırı push'tan bağımsız
    }

    @Test
    void rolledBackNotificationsSendNoPushAndLeaveNoRow() throws Exception {
        Long userId = newUser("rollback");
        String token = "push-token-" + UUID.randomUUID();
        deviceTokenService.register(userId, new DeviceRegisterDto(token, DevicePlatform.ANDROID, "tr", "1.0"));

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            notificationService.upsertForExecution(draft(userId, "exec:2:REMINDER"));
            throw new IllegalStateException("rollback");
        }));

        Thread.sleep(800); // asenkron gönderim olsaydı bu sürede gelirdi
        assertTrue(SENT.stream().noneMatch(s -> s.token().equals(token)));
        assertEquals(0, notificationService.unreadCount(userId).getCount());
    }

    @Test
    void usersWithoutDevicesOrWithPushOffGetOnlyTheInboxRow() throws Exception {
        Long noDevice = newUser("nodevice");
        Long pushOff = newUser("pushoff");
        String token = "push-token-" + UUID.randomUUID();
        deviceTokenService.register(pushOff, new DeviceRegisterDto(token, DevicePlatform.ANDROID, "tr", "1.0"));
        jdbc.update("""
                INSERT INTO notification_preferences
                    (user_id, push_enabled, reminders_enabled, supporter_alerts_enabled, nudges_enabled,
                     hide_details_in_push, created_at, updated_at)
                VALUES (?, false, true, true, true, false, now(), now())
                """, pushOff);

        notificationService.upsertForExecution(draft(noDevice, "exec:3:REMINDER"));
        notificationService.upsertForExecution(draft(pushOff, "exec:3:REMINDER"));

        Thread.sleep(800);
        assertTrue(SENT.isEmpty());
        assertEquals(1, notificationService.unreadCount(noDevice).getCount());
        assertEquals(1, notificationService.unreadCount(pushOff).getCount());
    }

    private static NotificationDraft draft(Long userId, String key) {
        return new NotificationDraft(userId, NotificationType.ROUTINE_REMINDER, key, null, null, null,
                NotificationParams.of("supplementName", "Çinko", "minutes", 10, "phase", "PRE"));
    }

    private Sent awaitSent(String token) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            for (Sent sent : SENT) {
                if (sent.token().equals(token)) {
                    return sent;
                }
            }
            Thread.sleep(100);
        }
        return fail("push was not sent within 5 seconds");
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
