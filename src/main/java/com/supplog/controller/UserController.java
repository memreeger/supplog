package com.supplog.controller;

import com.supplog.dto.user.*;
import com.supplog.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /*
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void addUser(@Valid @RequestBody CreateUserRequestDto createUserRequestDto) {
        userService.addUser(createUserRequestDto);
    }

     */

    @GetMapping("/{id}")
    UserResponseDto getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/email/{email}")
    UserResponseDto getByEmail(@PathVariable String email) {
        return userService.getByEmail(email);
    }

    @GetMapping
    List<UserResponseDto> getAll() {
        return userService.getAll();
    }

    @GetMapping("/getAllActiveUsers")
    List<UserResponseDto> getAllActiveUsers() {
        return userService.getAllActiveUsers();
    }

    @PutMapping("/email/{email}/password")
    void changePasswordByEmail(
            @PathVariable String email,
            @Valid @RequestBody ChangePasswordRequestDto passwordRequestDto) {
        userService.changePasswordByEmail(email, passwordRequestDto);
    }

    @PutMapping("/email/{email}/profile")
    void updateUserInfoByEmail(
            @PathVariable String email,
            @Valid @RequestBody UpdateUserProfileRequestDto userProfileRequestDto) {
        userService.updateUserInfoByEmail(email, userProfileRequestDto);
    }

    @PutMapping("/{id}/profile")
    void updateUserInfoById(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserProfileRequestDto userProfileRequestDto) {
        userService.updateUserInfoById(id, userProfileRequestDto);
    }

    @PutMapping("/username/{userName}/password")
    void changePasswordByUserName(
            @PathVariable String userName,
            @Valid @RequestBody ChangePasswordRequestDto passwordRequestDto) {
        userService.changePasswordByUserName(userName, passwordRequestDto);
    }

    @PutMapping("/{id}/password")
    void changePasswordById(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDto changePasswordRequestDto) {
        userService.changePasswordById(id, changePasswordRequestDto);
    }

    @PutMapping("/username/{userName}/profile")
    void updateUserInfoByUserName(
            @PathVariable String userName,
            @Valid @RequestBody UpdateUserProfileRequestDto userProfileRequestDto) {
        userService.updateUserInfoByUserName(userName, userProfileRequestDto);
    }

    @DeleteMapping("/email/{email}")
    void deleteUserByEmail(
            @PathVariable String email,
            @Valid @RequestBody DeleteUserRequestDto userRequestDto) {
        userService.deleteUserByEmail(email, userRequestDto);
    }

    @DeleteMapping("/{id}")
    void deleteUserById(
            @PathVariable Long id,
            @Valid @RequestBody DeleteUserRequestDto userRequestDto) {
        userService.deleteUserById(id, userRequestDto);
    }
}