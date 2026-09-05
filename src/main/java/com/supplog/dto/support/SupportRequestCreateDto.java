package com.supplog.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportRequestCreateDto {

    @NotBlank(message = "{validation.support.identifier.required}")
    private String identifier;
}