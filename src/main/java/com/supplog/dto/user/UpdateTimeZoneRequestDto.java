package com.supplog.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTimeZoneRequestDto {

    @NotBlank(
            message = "{validation.user.timezone.required}"
    )
    @Size(
            max = 64,
            message = "{validation.user.timezone.size}"
    )
    private String timeZone;
}