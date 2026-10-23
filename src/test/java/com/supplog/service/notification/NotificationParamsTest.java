package com.supplog.service.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationParamsTest {

    @Test
    void buildsMapFromKeyValuePairs() {
        Map<String, Object> params = NotificationParams.of("supplementName", "Çinko", "minutes", 10);

        assertEquals("Çinko", params.get("supplementName"));
        assertEquals(10, params.get("minutes"));
    }

    @Test
    void skipsNullValuesAndConvertsInstantToIsoString() {
        Instant at = Instant.parse("2026-10-23T05:00:00Z");

        Map<String, Object> params = NotificationParams.of("ownerName", null, "scheduledAt", at);

        assertFalse(params.containsKey("ownerName"));
        assertEquals("2026-10-23T05:00:00Z", params.get("scheduledAt"));
    }

    @Test
    void rejectsOddArgumentCountAndBlankKeys() {
        assertThrows(IllegalArgumentException.class, () -> NotificationParams.of("onlyKey"));
        assertThrows(IllegalArgumentException.class, () -> NotificationParams.of(" ", "value"));
        assertThrows(IllegalArgumentException.class, () -> NotificationParams.of(1, "value"));
    }
}
