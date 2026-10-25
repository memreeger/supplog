package com.supplog.controller;

import com.supplog.dto.notification.ReminderSettingsDto;
import com.supplog.service.notification.ReminderSettingsService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/routines/{routineId}/reminder-settings")
@RequiredArgsConstructor
public class ReminderSettingsController {

    private final ReminderSettingsService reminderSettingsService;

    @GetMapping
    public ResponseEntity<ReminderSettingsDto> get(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable @Positive(message = "{validation.id.positive}") Long routineId
    ) {
        return ResponseEntity.ok(reminderSettingsService.get(currentUser.getId(), routineId));
    }

    @PutMapping
    public ResponseEntity<ReminderSettingsDto> update(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable @Positive(message = "{validation.id.positive}") Long routineId,
            @Valid @RequestBody ReminderSettingsDto request
    ) {
        return ResponseEntity.ok(reminderSettingsService.update(currentUser.getId(), routineId, request));
    }
}
