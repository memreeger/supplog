package com.supplog.controller;


import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDto register(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDto login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        return authService.login(request);
    }
}
