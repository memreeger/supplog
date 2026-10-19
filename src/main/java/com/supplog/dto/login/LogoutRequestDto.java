package com.supplog.dto.login;

import jakarta.validation.constraints.Size;

// Logout idempotenttir; refresh token boş/geçersiz olsa da 204 döner.
public record LogoutRequestDto(

        @Size(max = 200, message = "{validation.refresh.token.required}")
        String refreshToken
) {
}
