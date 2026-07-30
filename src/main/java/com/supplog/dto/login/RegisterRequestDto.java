package com.supplog.dto.login;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequestDto(

        @NotBlank(message = "{validation.firstname.required}")
        @Size(min = 2, max = 50, message = "{validation.firstname.size}")
        String firstName,

        @NotBlank(message = "{validation.lastname.required}")
        @Size(min = 2, max = 50, message = "{validation.lastname.size}")
        String lastName,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 254, message = "{validation.email.size}")
        String email,

        @NotBlank(message = "{validation.username.required}")
        @Size(min = 3, max = 30, message = "{validation.username.size}")
        String username,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 50, message = "{validation.password.size}")
        String password,

        @NotNull(message = "{validation.birthDate.required}")
        @Past(message = "{validation.birthDate.mustBePast}")
        LocalDate birthDate

) {
}