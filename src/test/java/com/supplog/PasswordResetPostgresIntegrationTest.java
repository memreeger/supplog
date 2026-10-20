package com.supplog;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.enums.ClientPlatform;
import com.supplog.exception.AuthTokenException;
import com.supplog.exception.PasswordResetTokenInvalidException;
import com.supplog.service.auth.AuthService;
import com.supplog.service.auth.ClientInfo;
import com.supplog.service.auth.PasswordResetService;
import com.supplog.service.mail.MailMessage;
import com.supplog.service.mail.MailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class PasswordResetPostgresIntegrationTest {

    private static final String PASSWORD = "Test-pass-123";
    private static final String NEW_PASSWORD = "Brand-new-456";
    private static final ClientInfo CLIENT =
            new ClientInfo(null, ClientPlatform.WEB, "127.0.0.1", "junit");
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AuthService authService;
    @Autowired PasswordResetService passwordResetService;

    @MockitoBean MailSender mailSender;

    @Test
    void fullResetFlowChangesPasswordAndEndsSessions() {
        String username = insertUser("reset");
        AuthResponseDto session = login(username, PASSWORD);

        passwordResetService.requestReset(username.toUpperCase() + "@EXAMPLE.TEST", "127.0.0.1");

        // Mail commit sonrası async gider.
        ArgumentCaptor<MailMessage> mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender, timeout(5_000)).send(mail.capture());
        assertEquals(username + "@example.test", mail.getValue().to());
        String rawToken = extractToken(mail.getValue().textBody());

        String storedHash = jdbc.queryForObject(
                "SELECT token_hash FROM password_reset_tokens WHERE user_id = ?", String.class, userId(username));
        assertNotEquals(rawToken, storedHash);

        passwordResetService.resetPassword(rawToken, NEW_PASSWORD, NEW_PASSWORD);

        assertThrows(BadCredentialsException.class, () -> login(username, PASSWORD));
        assertNotNull(login(username, NEW_PASSWORD).accessToken());
        assertThrows(AuthTokenException.class, () -> authService.refresh(session.refreshToken(), CLIENT));
        assertThrows(PasswordResetTokenInvalidException.class,
                () -> passwordResetService.resetPassword(rawToken, "Another-789x", "Another-789x"));
    }

    @Test
    void newRequestInvalidatesPreviousLink() {
        String username = insertUser("relink");

        passwordResetService.requestReset(username + "@example.test", null);
        passwordResetService.requestReset(username + "@example.test", null);

        ArgumentCaptor<MailMessage> mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailSender, timeout(5_000).times(2)).send(mail.capture());
        String firstToken = extractToken(mail.getAllValues().get(0).textBody());
        String secondToken = extractToken(mail.getAllValues().get(1).textBody());
        String[] tokens = {firstToken, secondToken};

        // Async teslim sırası garanti değil: hangisinin ikinci talep olduğu DB'den bulunur.
        String openHash = jdbc.queryForObject(
                "SELECT token_hash FROM password_reset_tokens WHERE user_id = ? AND used_at IS NULL",
                String.class, userId(username));
        String latest = com.supplog.util.TokenHashing.sha256Hex(tokens[0]).equals(openHash) ? tokens[0] : tokens[1];
        String stale = latest.equals(tokens[0]) ? tokens[1] : tokens[0];

        assertThrows(PasswordResetTokenInvalidException.class,
                () -> passwordResetService.resetPassword(stale, NEW_PASSWORD, NEW_PASSWORD));
        passwordResetService.resetPassword(latest, NEW_PASSWORD, NEW_PASSWORD);
    }

    @Test
    void unknownEmailSendsNothing() {
        passwordResetService.requestReset("missing-" + UUID.randomUUID() + "@example.test", null);

        verify(mailSender, after(500).never()).send(any());
    }

    private static String extractToken(String body) {
        Matcher matcher = TOKEN.matcher(body);
        assertTrue(matcher.find(), "reset link missing from mail");
        return matcher.group(1);
    }

    private AuthResponseDto login(String username, String password) {
        return authService.login(new LoginRequestDto(username, password, "junit", ClientPlatform.WEB), CLIENT);
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
}
