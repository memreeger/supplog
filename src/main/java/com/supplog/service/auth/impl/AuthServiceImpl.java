package com.supplog.service.auth.impl;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.entity.User;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.UserRepository;
import com.supplog.service.auth.AuthService;
import com.supplog.service.user.impl.JwtService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private final ModelMapper modelMapper;


    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.modelMapper = modelMapper;
    }

    @Override
    public AuthResponseDto register(RegisterRequestDto request) {

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BusinessException("user.username.already.exists");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("user.email.already.exists");
        }

        User user = modelMapper.map(request, User.class);

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setScore(0);
        user.setDeleted(false);

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateToken(savedUser.getUsername());

        return new AuthResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                accessToken,
                "Bearer"
        );
    }


    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("user.username.not.found"));

        if (user.isDeleted()) {
            throw new BusinessException("user.already.deleted");
        }

        String accessToken = jwtService.generateToken(user.getUsername());

        return new AuthResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessToken,
                "Bearer"
        );
    }
}
