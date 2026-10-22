package com.supplog;

import com.supplog.dto.support.SupportAccessUpdateDto;
import com.supplog.dto.support.SupportConnectionResponseDto;
import com.supplog.dto.support.SupportRequestCreateDto;
import com.supplog.dto.support.SupportRequestResponseDto;
import com.supplog.dto.support.SupportRoutineResponseDto;
import com.supplog.dto.support.SupportRoutineSelectionUpdateDto;
import com.supplog.enums.SupportAccessScope;
import com.supplog.service.support.SupportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Takip Çevrem: destekçinin gördüğü rutinler, kapsam (tümü / seçili) ve seçimle birlikte değişir.
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class SupportVisibilityPostgresIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SupportService supportService;

    @Test
    void newConnectionSharesAllRoutinesAndScopeChangesControlWhatTheSupporterSees() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long ownerId = insertUser("owner-" + suffix);
        Long supporterId = insertUser("supporter-" + suffix);
        Long supplementId = insertSupplement(ownerId);
        Long first = insertRoutine(ownerId, supplementId, "08:00");
        Long second = insertRoutine(ownerId, supplementId, "21:00");

        // Rutin sahibi davet eder, davet edilen kabul eder.
        SupportRequestCreateDto invite = new SupportRequestCreateDto();
        invite.setIdentifier("supporter-" + suffix);
        SupportRequestResponseDto request = supportService.sendRequest(ownerId, invite);
        SupportConnectionResponseDto connection = supportService.acceptRequest(supporterId, request.getId());

        // Varsayılan: tüm rutinler görünür (eskiden seçim boştu ve destekçi hiçbir şey görmüyordu).
        assertEquals(SupportAccessScope.ALL_ROUTINES, connection.getAccessScope());
        assertEquals(Set.of(first, second), visibleTo(supporterId, connection.getId()));

        // Tümü -> seçili: backend seçimi temizler; seçim yapılana kadar hiçbir şey paylaşılmaz.
        supportService.updateAccessScope(ownerId, connection.getId(), scope(SupportAccessScope.SELECTED_ROUTINES));
        assertEquals(Set.of(), visibleTo(supporterId, connection.getId()));

        supportService.updateSelectedRoutines(ownerId, connection.getId(), selection(first));
        assertEquals(Set.of(first), visibleTo(supporterId, connection.getId()));

        // Seçili -> tümü -> seçili: seçim her geçişte sıfırlanır.
        supportService.updateAccessScope(ownerId, connection.getId(), scope(SupportAccessScope.ALL_ROUTINES));
        assertEquals(Set.of(first, second), visibleTo(supporterId, connection.getId()));
        supportService.updateAccessScope(ownerId, connection.getId(), scope(SupportAccessScope.SELECTED_ROUTINES));
        assertEquals(Set.of(), visibleTo(supporterId, connection.getId()));
        assertEquals(Set.of(), supportService.getRoutineSelection(ownerId, connection.getId()).getRoutineIds());
    }

    private Set<Long> visibleTo(Long supporterId, Long connectionId) {
        List<SupportRoutineResponseDto> routines = supportService.getSharedRoutines(supporterId, connectionId);
        return routines.stream().map(SupportRoutineResponseDto::getRoutineId).collect(java.util.stream.Collectors.toSet());
    }

    private static SupportAccessUpdateDto scope(SupportAccessScope scope) {
        SupportAccessUpdateDto dto = new SupportAccessUpdateDto();
        dto.setAccessScope(scope);
        return dto;
    }

    private static SupportRoutineSelectionUpdateDto selection(Long... ids) {
        SupportRoutineSelectionUpdateDto dto = new SupportRoutineSelectionUpdateDto();
        dto.setRoutineIds(Set.of(ids));
        return dto;
    }

    private Long insertUser(String username) {
        Long id = jdbc.queryForObject("""
                INSERT INTO users
                    (birth_date, created_at, e_mail, first_name, is_deleted, last_name,
                     password, score, updated_at, user_name, token_version, time_zone,
                     must_change_password)
                VALUES (?, ?, ?, 'Test', false, 'User', ?, 0, ?, ?, 0, 'UTC', false)
                RETURNING id
                """, Long.class,
                LocalDate.of(1990, 1, 1), LocalDateTime.now(),
                username + "@example.test", passwordEncoder.encode("Test-pass-123"),
                LocalDateTime.now(), username);
        jdbc.update("INSERT INTO users_roles (user_id, role_id) SELECT ?, id FROM role WHERE name = 'ROLE_USER'", id);
        return id;
    }

    private Long insertSupplement(Long ownerId) {
        return jdbc.queryForObject("""
                INSERT INTO supplements
                    (category, created_at, is_deleted, supplement_name, supplement_dosage, updated_at, inserted_by_user_id)
                VALUES ('SUPPLEMENT', ?, false, 'Zinc', '5 ml', ?, ?)
                RETURNING id
                """, Long.class, LocalDateTime.now(), LocalDateTime.now(), ownerId);
    }

    private Long insertRoutine(Long ownerId, Long supplementId, String time) {
        return jdbc.queryForObject("""
                INSERT INTO routines
                    (created_at, duration_type, frequency, is_deleted, routine_time, start_date,
                     updated_at, supplement_id, user_id, missed_grace_period)
                VALUES (?, 'LIFE_TIME', 'DAILY', false, ?::time, ?, ?, ?, ?, 'THIRTY_MINUTES')
                RETURNING id
                """, Long.class, LocalDateTime.now(), time, LocalDate.now(), LocalDateTime.now(), supplementId, ownerId);
    }
}
