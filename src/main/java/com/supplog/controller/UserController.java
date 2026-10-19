package com.supplog.controller;

import com.supplog.dto.user.*;
import com.supplog.security.RefreshTokenCookieService;
import jakarta.servlet.http.HttpServletResponse;
import com.supplog.service.user.UserService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public UserController(
            UserService userService,
            RefreshTokenCookieService refreshTokenCookieService
    ) {
        this.userService = userService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDto myProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return userService.getMyProfile(currentUser.getId());
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal CustomUserDetails currentUser, @Valid @RequestBody ChangePasswordRequestDto requestDto) {
        userService.changeMyPassword(
                currentUser.getId(),
                requestDto
        );
    }

    @PostMapping("/me/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletResponse response
    ) {
        userService.logoutAllSessions(currentUser.getId());
        refreshTokenCookieService.clearToken(response);
    }

    @PatchMapping("/me/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser, @Valid @RequestBody UpdateUserProfileRequestDto requestDto) {
        userService.updateMyProfile(
                currentUser.getId(),
                requestDto
        );
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@AuthenticationPrincipal CustomUserDetails currentUser, @Valid @RequestBody DeleteUserRequestDto requestDto) {
        userService.deActivateMyProfile(
                currentUser.getId(),
                requestDto
        );
    }

    @PatchMapping("/me/time-zone")
    public ResponseEntity<Void> updateMyTimeZone(
            @AuthenticationPrincipal
            CustomUserDetails currentUser,

            @Valid
            @RequestBody
            UpdateTimeZoneRequestDto request
    ) {

        userService.updateMyTimeZone(
                currentUser.getId(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}