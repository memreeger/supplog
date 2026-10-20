package com.supplog.dto.login;

import jakarta.validation.constraints.Size;

// Sadece mobil istemci gönderir; web istemcide token httpOnly cookie'den okunur.
public record RefreshTokenRequestDto(

        @Size(max = 200, message = "{validation.refresh.token.required}")
        String refreshToken
) {
}
