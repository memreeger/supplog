package com.supplog.dto.login;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequestDto(
        @NotBlank
        @Size(min = 2, max = 50)
        String firstName,

        @NotBlank
        @Size(min = 2, max = 50)
        String lastName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 3, max = 30)
        String username,

        @NotBlank
        @Size(min = 6, max = 50)
        String password,

        @NotNull
        @Past
        LocalDate birthDate
) {
}