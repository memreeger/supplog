package com.supplog.controller;

import com.supplog.dto.notification.NotificationPreferenceDto;
import com.supplog.dto.notification.NotificationResponseDto;
import com.supplog.dto.notification.UnreadCountResponseDto;
import com.supplog.service.notification.NotificationPreferenceService;
import com.supplog.service.notification.NotificationService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> list(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(notificationService.list(currentUser.getId(), limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDto> unreadCount(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(notificationService.unreadCount(currentUser.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable @Positive Long id
    ) {
        notificationService.markRead(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> getPreferences(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(preferenceService.get(currentUser.getId()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody NotificationPreferenceDto request
    ) {
        return ResponseEntity.ok(preferenceService.update(currentUser.getId(), request));
    }
}
