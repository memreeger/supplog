package com.supplog.controller;


import com.supplog.dto.user.CreateUserRequestDto;
import com.supplog.entity.User;
import com.supplog.service.user.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping
    void addUser(@RequestBody CreateUserRequestDto createUserRequestDto) {
        userService.addUser(createUserRequestDto);
    }
}
