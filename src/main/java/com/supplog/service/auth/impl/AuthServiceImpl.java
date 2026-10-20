package com.supplog.service.auth.impl;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.entity.Role;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.exception.AuthTokenException;
import com.supplog.exception.BusinessException;
import com.supplog.exception.ResourceNotFoundException;
import com.supplog.repository.RoleRepository;
import com.supplog.repository.UserRepository;
import com.supplog.service.auth.AuthService;
import com.supplog.service.auth.ClientInfo;
import com.supplog.service.auth.IssuedRefreshToken;
import com.supplog.service.auth.RefreshTokenService;
import com.supplog.service.user.impl.CustomUserDetails;
import com.supplog.service.user.impl.JwtService;
import com.supplog.util.InputNormalizer;
import com.supplog.util.TimeZoneResolver;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    private final TimeZoneResolver timeZoneResolver;

    private final RefreshTokenService refreshTokenService;


    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, ModelMapper modelMapper, RoleRepository roleRepository, TimeZoneResolver timeZoneResolver, RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.modelMapper = modelMapper;
        this.roleRepository = roleRepository;
        this.timeZoneResolver = timeZoneResolver;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request, ClientInfo clientInfo) {
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
        user.setTimeZone(timeZoneResolver.normalize(
                request.timeZone()
        ));

        User savedUser = userRepository.save(user);

        return buildResponse(refreshTokenService.issue(savedUser, clientInfo));
    }


    //authentication principaldan user verilerini çek
    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request, ClientInfo clientInfo) {
        String username = InputNormalizer.normalizeUsername(request.username());


        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        request.password()
                )
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("auth.credentials.invalid"));

        ClientInfo loginClient = new ClientInfo(
                request.deviceName(),
                request.platform() != null ? request.platform() : clientInfo.platform(),
                clientInfo.ipAddress(),
                clientInfo.userAgent()
        );

        return buildResponse(refreshTokenService.issue(user, loginClient));
    }

    @Override
    @Transactional(noRollbackFor = AuthTokenException.class)
    public AuthResponseDto refresh(String refreshToken, ClientInfo clientInfo) {
        return buildResponse(refreshTokenService.rotate(refreshToken, clientInfo));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponseDto buildResponse(IssuedRefreshToken refreshToken) {
        User user = refreshToken.user();

        JwtService.IssuedAccessToken accessToken =
                jwtService.issueToken(user.getId(), user.getTokenVersion());

        return new AuthResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accessToken.token(),
                "Bearer",
                user.isMustChangePassword(),
                refreshToken.rawToken(),
                accessToken.expiresAt(),
                refreshToken.expiresAt()
        );
    }
}
