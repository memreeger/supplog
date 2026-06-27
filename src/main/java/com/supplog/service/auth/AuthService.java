package com.supplog.service.auth;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);
}