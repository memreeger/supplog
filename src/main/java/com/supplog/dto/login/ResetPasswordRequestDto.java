package com.supplog.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(

        @NotBlank(message = "{validation.password.reset.token.required}")
        @Size(max = 200, message = "{validation.password.reset.token.required}")
        String token,

        @NotBlank(message = "{validation.password.new.required}")
        @Size(min = 8, max = 50, message = "{validation.password.size}")
        String newPassword,

        @NotBlank(message = "{validation.password.confirm.required}")
        @Size(min = 8, max = 50, message = "{validation.password.size}")
        String confirmPassword
) {
}
