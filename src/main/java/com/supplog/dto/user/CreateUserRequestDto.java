package com.supplog.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequestDto extends BaseUserDto {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 254, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 50, message = "{validation.password.size}")
    private String password;

    @NotNull(message = "{validation.birthDate.required}")
    @Past(message = "{validation.birthDate.mustBePast}")
    private LocalDate birthDate;
}
