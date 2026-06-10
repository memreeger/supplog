package com.supplog.controller;

import com.supplog.dto.user.*;
import com.supplog.service.user.UserService;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void addUser(@RequestBody CreateUserRequestDto createUserRequestDto) {
        userService.addUser(createUserRequestDto);
    }

    @GetMapping("/{id}")
    UserResponseDto getById(@PathVariable Long id) {
        return userService.getById(id);
    }



    @GetMapping("/username/{userName}")
    UserResponseDto getByUserName(@PathVariable String userName) {
        return userService.getByUserName(userName);
    }

    @GetMapping("/email/{email}")
    UserResponseDto getByEmail(@PathVariable String email) {
        return userService.getByEmail(email);
    }

    @GetMapping
    List<UserResponseDto> getAll() {
        return userService.getAll();
    }

    @PutMapping("/email/{email}/password")
    void changePasswordByEmail(
            @PathVariable String email,
            @RequestBody ChangePasswordRequestDto passwordRequestDto) {
        userService.changePasswordByEmail(email, passwordRequestDto);
    }

    @PutMapping("/email/{email}/profile")
    void updateUserInfoByEmail(
            @PathVariable String email,
            @RequestBody UpdateUserProfileRequestDto userProfileRequestDto) {
        userService.updateUserInfoByEmail(email, userProfileRequestDto);
    }

    @PutMapping("/username/{userName}/password")
    void changePasswordByUserName(
            @PathVariable String userName,
            @RequestBody ChangePasswordRequestDto passwordRequestDto) {
        userService.changePasswordByUserName(userName, passwordRequestDto);
    }

    @PutMapping("/username/{userName}/profile")
    void updateUserInfoByUserName(
            @PathVariable String userName,
            @RequestBody UpdateUserProfileRequestDto userProfileRequestDto) {
        userService.updateUserInfoByUserName(userName, userProfileRequestDto);
    }

    @DeleteMapping("/email/{email}")
    void deleteUserByEmail(
            @PathVariable String email,
            @RequestBody DeleteUserRequestDto userRequestDto) {
        userService.deleteUserByEmail(email, userRequestDto);
    }
}