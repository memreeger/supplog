package com.supplog.util;

import java.util.Locale;

public final class InputNormalizer {
    private InputNormalizer() {
    }

    public static String normalizeUsername(String username) {
        return username == null
                ? null
                : username.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeEmail(String email) {
        return email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String trim(String value) {
        return value == null
                ? null
                : value.trim();
    }
}
