package com.supplog.service.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bildirimin {@code params} JSON'unu güvenli kurar: NotificationParams.of("supplementName", "Çinko", "minutes", 10).
 * Null değerler yazılmaz, Instant ISO-8601 metnine çevrilir; istemci metni bu verilerle kendi dilinde çizer.
 */
public final class NotificationParams {

    private NotificationParams() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("params must be key/value pairs");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (!(keyValues[i] instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException("params key must be a non-blank string");
            }
            Object value = keyValues[i + 1];
            if (value == null) {
                continue;
            }
            params.put(key, value instanceof Instant instant ? instant.toString() : value);
        }
        return params;
    }
}
