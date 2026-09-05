package com.supplog.dto.support;

import com.supplog.enums.SupportAccessScope;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportAccessUpdateDto {

    @NotNull(message = "{validation.support.access.scope.required}")
    private SupportAccessScope accessScope;
}