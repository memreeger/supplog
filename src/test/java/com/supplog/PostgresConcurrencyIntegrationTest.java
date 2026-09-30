package com.supplog;

import com.supplog.dto.admin.execution.AdminExecutionCorrectionRequestDto;
import com.supplog.dto.routine.UpdateRoutineTimeRequestDto;
import com.supplog.dto.user.DeleteUserRequestDto;
import com.supplog.enums.RoutineExecutionStatus;
import com.supplog.service.admin.adminExecutionService.AdminExecutionService;
import com.supplog.service.routine.RoutineService;
import com.supplog.service.support.SupportService;
import com.supplog.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class PostgresConcurrencyIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserService userService;
    @Autowired SupportService supportService;
    @Autowired RoutineService routineService;
    @Autowired AdminExecutionService adminExecutionService;

    @Test
    void deactivationAndSupportAcceptanceCannotLeaveAcceptedRelationship() throws Exception {
        String password = "Test-pass-123";
        Long ownerId = insertUser("owner", password);
        Long supporterId = insertUser("supporter", password);
        Long relationshipId = jdbc.queryForObject("""
                INSERT INTO support_relationships
                    (access_scope, created_at, requested_at, status, updated_at,
                     supported_user_id, supporter_user_id)
                VALUES ('ALL_ROUTINES', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PENDING',
                        CURRENT_TIMESTAMP, ?, ?)
                RETURNING id
                """, Long.class, ownerId, supporterId);

        try {
            ConcurrentResult result = runConcurrently(
                    () -> userService.deActivateMyProfile(ownerId, new DeleteUserRequestDto(password)),
                    () -> supportService.acceptRequest(supporterId, relationshipId)
            );

            assertTrue(result.firstSucceeded());
            assertTrue(jdbc.queryForObject(
                    "SELECT is_deleted FROM users WHERE id = ?", Boolean.class, ownerId));
            assertNotEquals("ACCEPTED", jdbc.queryForObject(
                    "SELECT status FROM support_relationships WHERE id = ?",
                    String.class, relationshipId));
        } finally {
            jdbc.update("DELETE FROM support_relationships WHERE id = ?", relationshipId);
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", ownerId, supporterId);
        }
    }

    @Test
    void routineUpdateAndAdminCorrectionFinishWithoutDeadlock() throws Exception {
        Long ownerId = insertUser("routine-owner", "Test-pass-123");
        Long adminId = insertUser("admin", "Test-pass-123");
        Long supplementId = jdbc.queryForObject("""
                INSERT INTO supplements
                    (created_at, expire_date, is_deleted, supplement_name,
                     supplement_dosage, category, updated_at, inserted_by_user_id)
                VALUES (CURRENT_TIMESTAMP, CURRENT_DATE + 30, false, 'test', '1',
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
                    (created_at, scheduled_at, scheduled_date, scheduled_time,
                     scheduled_zone_id, status, updated_at, routine_id)
                VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_DATE, '08:00',
                        'UTC', 'PENDING', CURRENT_TIMESTAMP, ?)
                RETURNING id
                """, Long.class, routineId);

        try {
            ConcurrentResult result = runConcurrently(
                    () -> routineService.updateRoutineTime(
                            ownerId, routineId, new UpdateRoutineTimeRequestDto(LocalTime.of(9, 0))),
                    () -> adminExecutionService.correct(
                            adminId, executionId,
                            new AdminExecutionCorrectionRequestDto(
                                    RoutineExecutionStatus.COMPLETED, "concurrency test"))
            );

            assertTrue(result.firstSucceeded());
            assertTrue(result.secondSucceeded());
            assertEquals("COMPLETED", jdbc.queryForObject(
                    "SELECT status FROM routine_executions WHERE id = ?",
                    String.class, executionId));
        } finally {
            jdbc.update("DELETE FROM admin_audit_logs WHERE resource_id = ?", executionId);
            jdbc.update("DELETE FROM routine_executions WHERE id = ?", executionId);
            jdbc.update("DELETE FROM routines WHERE id = ?", routineId);
            jdbc.update("DELETE FROM supplements WHERE id = ?", supplementId);
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", ownerId, adminId);
        }
    }

    private Long insertUser(String prefix, String password) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return jdbc.queryForObject("""
                INSERT INTO users
                    (birth_date, created_at, e_mail, first_name, is_deleted, last_name,
                     password, score, updated_at, user_name, token_version, time_zone,
                     must_change_password)
                VALUES (?, ?, ?, 'Test', false, 'User', ?, 0, ?, ?, 0, 'UTC', false)
                RETURNING id
                """, Long.class,
                LocalDate.of(1990, 1, 1), LocalDateTime.now(),
                prefix + "-" + suffix + "@example.test", passwordEncoder.encode(password),
                LocalDateTime.now(), prefix + "-" + suffix);
    }

    private ConcurrentResult runConcurrently(ThrowingAction first, ThrowingAction second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> runAfter(start, first));
            var secondResult = executor.submit(() -> runAfter(start, second));
            start.countDown();
            return new ConcurrentResult(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)
            );
        }
    }

    private boolean runAfter(CountDownLatch start, ThrowingAction action) throws Exception {
        start.await();
        try {
            action.run();
            return true;
        } catch (RuntimeException expectedRaceOutcome) {
            return false;
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run();
    }

    private record ConcurrentResult(boolean firstSucceeded, boolean secondSucceeded) {
    }
}
