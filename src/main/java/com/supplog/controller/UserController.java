package com.supplog.controller;

import com.supplog.dto.user.*;
import com.supplog.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    UserResponseDto myProfile(Authentication authentication) {
        String username = authentication.getName();
        return userService.getMyProfile(username);
    }


    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequestDto changePasswordRequestDto) {
        String username = authentication.getName();
        userService.changeMyPassword(username, changePasswordRequestDto);
    }

    @PatchMapping("/me/update")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateProfile(Authentication authentication, @Valid @RequestBody UpdateUserProfileRequestDto userProfileRequestDto) {
        String username = authentication.getName();
        userService.updateMyProfile(username, userProfileRequestDto);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProfile(Authentication authentication, @Valid @RequestBody DeleteUserRequestDto deleteUserRequestDto) {
        String username = authentication.getName();
        userService.deActivateMyProfile(username, deleteUserRequestDto);
    }
}