package com.supplog.dto.login;

public record AuthResponseDto(
        Long id,
        String username,
        String email,
        String accessToken,
        String tokenType
) {
}