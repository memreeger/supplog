package com.supplog.service.auth.impl;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.entity.Role;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoleRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.auth.AuthService;
import com.supplog.service.user.impl.CustomUserDetails;
import com.supplog.service.user.impl.JwtService;
import com.supplog.util.InputNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {


    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private final ModelMapper modelMapper;

    private final RoleRepository roleRepository;


    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, ModelMapper modelMapper, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.modelMapper = modelMapper;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String username = InputNormalizer.normalizeUsername(request.username());
        String email = InputNormalizer.normalizeEmail(request.email());

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("user.username.already.exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("user.email.already.exists");
        }

        User user = modelMapper.map(request, User.class);

        user.setUsername(username);
        user.setEmail(email);

        Role role = roleRepository
                .findByName(RoleName.ROLE_USER)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "role.not.found"
                        )
                );
        user.getRoles().add(role);
        user.setFirstName(InputNormalizer.trim(request.firstName()));
        user.setLastName(InputNormalizer.trim(request.lastName()));
        user.setBirthDate(request.birthDate());

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setScore(0);
        user.setDeleted(false);

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateToken(savedUser.getUsername(), savedUser.getTokenVersion());

        return new AuthResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                accessToken,
                "Bearer"
        );
    }


    //authentication principaldan user verilerini çek
    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        String username = InputNormalizer.normalizeUsername(request.username());


        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        request.password()
                )
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        String accesToken = jwtService.generateToken(principal.getUsername(), principal.getTokenVersion());

        return new AuthResponseDto(
                principal.getId(),
                principal.getUsername(),
                principal.getEmail(),
                accesToken,
                "Bearer");


    }
}
