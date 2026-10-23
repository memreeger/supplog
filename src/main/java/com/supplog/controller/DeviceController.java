package com.supplog.controller;

import com.supplog.dto.notification.DeviceRegisterDto;
import com.supplog.service.notification.DeviceTokenService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/notifications/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    // Token'ı yol yerine gövdede gönderiyoruz: URL'ler loglanır, token bir sırdır.
    @PutMapping
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody DeviceRegisterDto request
    ) {
        deviceTokenService.register(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UnregisterRequest request
    ) {
        deviceTokenService.unregister(currentUser.getId(), request.pushToken());
        return ResponseEntity.noContent().build();
    }

    public record UnregisterRequest(@NotBlank @Size(max = 512) String pushToken) {
    }
}
