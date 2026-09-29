package com.supplog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReasonRequestDto(
        @NotBlank(message = "{validation.admin.reason.required}")
        @Size(max = 500, message = "{validation.admin.reason.size}")
        String reason
) {
}
