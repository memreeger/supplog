package com.supplog.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequestDto {
    @NotBlank(message = "{validation.password.required}")
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String newPassword;

    @NotBlank
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String confirmPassword;
}
