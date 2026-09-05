package com.supplog.dto.support;

import com.supplog.enums.SupportAccessScope;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class SupportRoutineSelectionResponseDto {

    private SupportAccessScope accessScope;
    private Set<Long> routineIds;
}