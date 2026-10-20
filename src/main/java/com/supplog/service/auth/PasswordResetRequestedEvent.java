package com.supplog.service.auth;

import java.util.Locale;

// Commit sonrası mail gönderimi için; rawToken sadece bellekte taşınır, loglanmaz.
public record PasswordResetRequestedEvent(
        String email,
        String firstName,
        String rawToken,
        Locale locale
) {

    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[email=" + email + ", locale=" + locale + "]";
    }
}
