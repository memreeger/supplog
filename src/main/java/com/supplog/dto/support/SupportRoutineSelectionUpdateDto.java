package com.supplog.dto.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class SupportRoutineSelectionUpdateDto {

    @NotNull(message = "{validation.support.routines.required}")
    private Set<@Positive(message = "{validation.id.positive}") Long> routineIds = new HashSet<>();
}