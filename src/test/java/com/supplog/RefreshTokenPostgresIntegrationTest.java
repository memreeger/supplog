package com.supplog;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.enums.ClientPlatform;
import com.supplog.exception.AuthTokenException;
import com.supplog.service.auth.AuthService;
import com.supplog.service.auth.ClientInfo;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class RefreshTokenPostgresIntegrationTest {

    private static final String PASSWORD = "Test-pass-123";
    private static final ClientInfo CLIENT =
            new ClientInfo(null, ClientPlatform.UNKNOWN, "127.0.0.1", "junit");

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AuthService authService;
    @Autowired UserService userService;

    @Test
    void refreshRotatesAndReuseAfterGraceRevokesFamily() {
        String username = insertUser("refresh");
        AuthResponseDto login = login(username);

        AuthResponseDto refreshed = authService.refresh(login.refreshToken(), CLIENT);
        assertNotEquals(login.refreshToken(), refreshed.refreshToken());

        // Eski token'ın rotation zamanını grace süresinin dışına çek.
        jdbc.update("""
                UPDATE refresh_tokens SET rotated_at = rotated_at - interval '1 minute'
                WHERE rotated_at IS NOT NULL AND user_id = ?
                """, userId(username));

        assertCode("REFRESH_TOKEN_REUSED", () -> authService.refresh(login.refreshToken(), CLIENT));
        assertCode("REFRESH_TOKEN_INVALID", () -> authService.refresh(refreshed.refreshToken(), CLIENT));
    }

    @Test
    void tokenVersionChangeRejectsRefresh() {
        String username = insertUser("version");
        AuthResponseDto login = login(username);

        // Şifre değişimi / admin işlemleri tokenVersion'ı artırır.
        jdbc.update("UPDATE users SET token_version = token_version + 1 WHERE id = ?", userId(username));

        assertCode("TOKEN_REVOKED", () -> authService.refresh(login.refreshToken(), CLIENT));
    }

    @Test
    void logoutAndLogoutAllInvalidateRefreshTokens() {
        String username = insertUser("logout");
        AuthResponseDto first = login(username);
        AuthResponseDto second = login(username);

        authService.logout(first.refreshToken());
        authService.logout(first.refreshToken());
        assertCode("REFRESH_TOKEN_INVALID", () -> authService.refresh(first.refreshToken(), CLIENT));

        userService.logoutAllSessions(userId(username));
        assertCode("REFRESH_TOKEN_INVALID", () -> authService.refresh(second.refreshToken(), CLIENT));
    }

    @Test
    void concurrentRefreshWithSameTokenSucceedsOnlyOnce() throws Exception {
        String username = insertUser("parallel");
        AuthResponseDto login = login(username);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> refreshAfter(start, login.refreshToken()));
            var second = executor.submit(() -> refreshAfter(start, login.refreshToken()));
            start.countDown();

            boolean firstSucceeded = first.get(10, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(10, TimeUnit.SECONDS);

            assertTrue(firstSucceeded ^ secondSucceeded);
        }

        Long activeCount = jdbc.queryForObject("""
                SELECT count(*) FROM refresh_tokens
                WHERE user_id = ? AND revoked_at IS NULL AND rotated_at IS NULL
                """, Long.class, userId(username));
        assertEquals(1L, activeCount);
    }

    private boolean refreshAfter(CountDownLatch start, String refreshToken) throws InterruptedException {
        start.await();
        try {
            authService.refresh(refreshToken, CLIENT);
            return true;
        } catch (AuthTokenException expectedRaceOutcome) {
            return false;
        }
    }

    private AuthResponseDto login(String username) {
        return authService.login(new LoginRequestDto(username, PASSWORD, "junit", ClientPlatform.WEB), CLIENT);
    }

    private Long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM users WHERE user_name = ?", Long.class, username);
    }

    private String insertUser(String prefix) {
        String username = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO users
                    (birth_date, created_at, e_mail, first_name, is_deleted, last_name,
                     password, score, updated_at, user_name, token_version, time_zone,
                     must_change_password)
                VALUES (?, ?, ?, 'Test', false, 'User', ?, 0, ?, ?, 0, 'UTC', false)
                """,
                LocalDate.of(1990, 1, 1), LocalDateTime.now(),
                username + "@example.test", passwordEncoder.encode(PASSWORD),
                LocalDateTime.now(), username);
        jdbc.update("""
                INSERT INTO users_roles (user_id, role_id)
                SELECT u.id, r.id FROM users u, role r
                WHERE u.user_name = ? AND r.name = 'ROLE_USER'
                """, username);
        return username;
    }

    private static void assertCode(String expectedCode, Runnable action) {
        AuthTokenException exception = assertThrows(AuthTokenException.class, action::run);
        assertEquals(expectedCode, exception.getCode());
    }
}
