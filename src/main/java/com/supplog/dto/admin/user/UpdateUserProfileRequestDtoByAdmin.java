package com.supplog.dto.admin.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfileRequestDtoByAdmin {
    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 30, message = "{validation.username.size}")
    private String username;

    @NotBlank(message = "{validation.firstname.required}")
    @Size(min = 2, max = 50, message = "{validation.firstname.size}")
    private String firstName;

    @NotBlank(message = "{validation.lastname.required}")
    @Size(min = 2, max = 50, message = "{validation.lastname.size}")
    private String lastName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 254, message = "{validation.email.size}")
    private String email;

    @NotNull(message = "{validation.birthDate.required}")
    @Past(message = "{validation.birthDate.mustBePast}")
    private LocalDate birthDate;
}
