package com.supplog.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateUserProfileRequestDto {

    @NotBlank(message = "{validation.firstname.required}")
    @Size(min = 2, max = 50, message = "{validation.firstname.size}")
    private String firstName;

    @NotBlank(message = "{validation.lastname.required}")
    @Size(min = 2, max = 50, message = "{validation.lastname.size}")
    private String lastName;
}
