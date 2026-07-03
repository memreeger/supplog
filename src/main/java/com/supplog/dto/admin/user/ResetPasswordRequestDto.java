package com.supplog.dto.admin.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequestDto {
    @NotBlank
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String newPassword;

    @NotBlank
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String confirmPassword;
}
