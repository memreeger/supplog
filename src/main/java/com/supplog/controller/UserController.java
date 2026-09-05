package com.supplog.controller;

import com.supplog.dto.user.*;
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

    public UserController(UserService userService) {
        this.userService = userService;
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