package com.supplog.dto.login;

public record AuthResponseDto(
        Long id,
        String userName,
        String email,
        String accessToken,
        String tokenType
) {
}