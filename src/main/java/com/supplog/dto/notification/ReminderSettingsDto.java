package com.supplog.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSettingsDto {

    @NotNull
    private Boolean enabled;

    // 10, 20, 30 veya 60; başka değer 400 döner.
    @NotNull
    private Integer leadMinutes;
}
