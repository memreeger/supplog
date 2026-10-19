package com.supplog.dto.login;

import java.time.Instant;

public record AuthResponseDto(
        Long id,
        String username,
        String email,
        String accessToken,
        String tokenType,
        boolean mustChangePassword,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt
) {

    // Web istemcide refresh token cookie ile gider, body'de görünmez.
    public AuthResponseDto withoutRefreshToken() {
        return new AuthResponseDto(
                id,
                username,
                email,
                accessToken,
                tokenType,
                mustChangePassword,
                null,
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
    }
}
