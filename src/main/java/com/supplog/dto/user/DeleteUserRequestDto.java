package com.supplog.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteUserRequestDto {

    /*
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

     */

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, max = 50, message = "{validation.password.size}")
    private String password;
}
