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
public class NotificationPreferenceDto {

    @NotNull
    private Boolean pushEnabled;

    @NotNull
    private Boolean remindersEnabled;

    @NotNull
    private Boolean supporterAlertsEnabled;

    @NotNull
    private Boolean nudgesEnabled;

    @NotNull
    private Boolean hideDetailsInPush;
}
