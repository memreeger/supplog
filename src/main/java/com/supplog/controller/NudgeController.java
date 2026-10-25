package com.supplog.controller;

import com.supplog.dto.notification.NudgeCreateDto;
import com.supplog.dto.notification.NudgeMuteResponseDto;
import com.supplog.dto.notification.NudgeResponseDto;
import com.supplog.service.notification.NudgeService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/nudges")
@RequiredArgsConstructor
public class NudgeController {

    private final NudgeService nudgeService;

    @PostMapping
    public ResponseEntity<NudgeResponseDto> send(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody NudgeCreateDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nudgeService.send(currentUser.getId(), request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<String>> templates() {
        return ResponseEntity.ok(nudgeService.templateKeys());
    }

    @GetMapping("/mutes")
    public ResponseEntity<List<NudgeMuteResponseDto>> mutes(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(nudgeService.listMutes(currentUser.getId()));
    }

    @PutMapping("/mutes/{senderUserId}")
    public ResponseEntity<Void> mute(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable @Positive Long senderUserId
    ) {
        nudgeService.mute(currentUser.getId(), senderUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/mutes/{senderUserId}")
    public ResponseEntity<Void> unmute(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable @Positive Long senderUserId
    ) {
        nudgeService.unmute(currentUser.getId(), senderUserId);
        return ResponseEntity.noContent().build();
    }
}
