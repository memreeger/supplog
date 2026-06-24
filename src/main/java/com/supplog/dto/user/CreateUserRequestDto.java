package com.supplog.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateUserRequestDto extends BaseUserDto {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String password;

    @NotNull(message = "{validation.birthDate.required}")
    @Past(message = "{validation.birthDate.mustBePast}")
    private LocalDate birthDate;
}
