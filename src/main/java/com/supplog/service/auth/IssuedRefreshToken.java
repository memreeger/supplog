package com.supplog.service.auth;

import com.supplog.entity.User;

import java.time.Instant;

// rawToken sadece istemciye bir kez döner, DB'de hash'i tutulur.
public record IssuedRefreshToken(
        User user,
        String rawToken,
        Instant expiresAt
) {
}
