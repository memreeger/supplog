package com.supplog;

import com.supplog.dto.notification.DeviceRegisterDto;
import com.supplog.enums.DevicePlatform;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class DevicePostgresIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired DeviceTokenService deviceTokenService;
    @Autowired UserService userService;

    @Test
    void registeringTheSameTokenTwiceKeepsOneRowAndRefreshesIt() {
        Long userId = newUser("reg");
        String token = token();

        deviceTokenService.register(userId, device(token, DevicePlatform.ANDROID, "tr_TR"));
        deviceTokenService.register(userId, device(token, DevicePlatform.ANDROID, "en"));

        assertEquals(1, deviceTokenService.tokensFor(userId).size());
        assertEquals("en", deviceTokenService.tokensFor(userId).getFirst().getLocale());
    }

    @Test
    void localeIsNormalizedAndOptional() {
        Long userId = newUser("loc");
        deviceTokenService.register(userId, device(token(), DevicePlatform.IOS, "tr_TR"));
        deviceTokenService.register(userId, device(token(), DevicePlatform.WEB, null));

        var locales = deviceTokenService.tokensFor(userId).stream().map(d -> d.getLocale()).toList();
        assertTrue(locales.contains("tr-TR"));
        assertTrue(locales.contains(null));
    }

    @Test
    void aTokenMovesToTheNewUserWhenTheDeviceIsShared() {
        Long first = newUser("first");
        Long second = newUser("second");
        String token = token();

        deviceTokenService.register(first, device(token, DevicePlatform.ANDROID, "tr"));
        deviceTokenService.register(second, device(token, DevicePlatform.ANDROID, "tr"));

        assertTrue(deviceTokenService.tokensFor(first).isEmpty(), "previous user must not get pushes on this device");
        assertEquals(1, deviceTokenService.tokensFor(second).size());
    }

    @Test
    void onlyTheOwnerCanUnregisterAToken() {
        Long owner = newUser("owner");
        Long stranger = newUser("stranger");
        String token = token();
        deviceTokenService.register(owner, device(token, DevicePlatform.ANDROID, "tr"));

        deviceTokenService.unregister(stranger, token);
        assertEquals(1, deviceTokenService.tokensFor(owner).size());

        deviceTokenService.unregister(owner, token);
        deviceTokenService.unregister(owner, token); // idempotent
        assertTrue(deviceTokenService.tokensFor(owner).isEmpty());
    }

    @Test
    void keepsOnlyTheNewestTwentyDevicesPerUser() {
        Long userId = newUser("many");
        String oldest = token();
        deviceTokenService.register(userId, device(oldest, DevicePlatform.ANDROID, "tr"));
        for (int i = 0; i < 20; i++) {
            deviceTokenService.register(userId, device(token(), DevicePlatform.ANDROID, "tr"));
        }

        assertEquals(20, deviceTokenService.tokensFor(userId).size());
        assertTrue(deviceTokenService.tokensFor(userId).stream().noneMatch(d -> d.getPushToken().equals(oldest)));
    }

    @Test
    void staleTokensAreDeletedAndFreshOnesKept() {
        Long userId = newUser("stale");
        String stale = token();
        String fresh = token();
        deviceTokenService.register(userId, device(stale, DevicePlatform.ANDROID, "tr"));
        deviceTokenService.register(userId, device(fresh, DevicePlatform.ANDROID, "tr"));
        jdbc.update("UPDATE device_tokens SET last_seen_at = now() - interval '90 days' WHERE push_token = ?", stale);

        deviceTokenService.deleteStale();

        var remaining = deviceTokenService.tokensFor(userId).stream().map(d -> d.getPushToken()).toList();
        assertEquals(java.util.List.of(fresh), remaining);
    }

    @Test
    void invalidTokensReportedByFcmAreRemovedForAnyOwner() {
        Long userId = newUser("invalid");
        String token = token();
        deviceTokenService.register(userId, device(token, DevicePlatform.ANDROID, "tr"));

        deviceTokenService.removeInvalid(token);

        assertTrue(deviceTokenService.tokensFor(userId).isEmpty());
    }

    @Test
    void loggingOutFromAllDevicesRemovesEveryPushToken() {
        Long userId = newUser("logoutall");
        deviceTokenService.register(userId, device(token(), DevicePlatform.ANDROID, "tr"));
        deviceTokenService.register(userId, device(token(), DevicePlatform.IOS, "en"));
        Long other = newUser("bystander");
        deviceTokenService.register(other, device(token(), DevicePlatform.ANDROID, "tr"));

        userService.logoutAllSessions(userId);

        assertTrue(deviceTokenService.tokensFor(userId).isEmpty());
        assertEquals(1, deviceTokenService.tokensFor(other).size());
    }

    private static String token() {
        return "test-token-" + UUID.randomUUID();
    }

    private static DeviceRegisterDto device(String token, DevicePlatform platform, String locale) {
        return new DeviceRegisterDto(token, platform, locale, "1.0.0");
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
