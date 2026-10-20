package com.supplog.controller;


import com.supplog.dto.login.AuthResponseDto;
import com.supplog.dto.login.ForgotPasswordRequestDto;
import com.supplog.dto.login.LoginRequestDto;
import com.supplog.dto.login.LogoutRequestDto;
import com.supplog.dto.login.RefreshTokenRequestDto;
import com.supplog.dto.login.RegisterRequestDto;
import com.supplog.dto.login.ResetPasswordRequestDto;
import com.supplog.enums.ClientPlatform;
import com.supplog.exception.AuthTokenException;
import com.supplog.security.ClientIpResolver;
import com.supplog.security.RefreshTokenCookieService;
import com.supplog.security.RequestOriginValidator;
import com.supplog.service.auth.AuthService;
import com.supplog.service.auth.ClientInfo;
import com.supplog.service.auth.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Web istemci: refresh token httpOnly cookie ile taşınır, body'de dönmez.
// Mobil istemci (X-Client-Type: mobile): refresh token body ile taşınır, cookie okunmaz.
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final RequestOriginValidator requestOriginValidator;
    private final ClientIpResolver clientIpResolver;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            RefreshTokenCookieService refreshTokenCookieService,
            RequestOriginValidator requestOriginValidator,
            ClientIpResolver clientIpResolver,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.requestOriginValidator = requestOriginValidator;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDto register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return deliver(
                authService.register(request, clientInfo(httpRequest)),
                httpRequest,
                httpResponse
        );
    }

    @PostMapping("/login")
    public AuthResponseDto login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return deliver(
                authService.login(request, clientInfo(httpRequest)),
                httpRequest,
                httpResponse
        );
    }

    @PostMapping("/refresh")
    public AuthResponseDto refresh(
            @Valid @RequestBody(required = false) RefreshTokenRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (refreshTokenCookieService.isMobileClient(httpRequest)) {
            return authService.refresh(
                    request != null ? request.refreshToken() : null,
                    clientInfo(httpRequest)
            );
        }

        requestOriginValidator.requireAllowedOrigin(httpRequest);

        String refreshToken = refreshTokenCookieService.readToken(httpRequest).orElse(null);

        try {
            return deliver(
                    authService.refresh(refreshToken, clientInfo(httpRequest)),
                    httpRequest,
                    httpResponse
            );
        } catch (AuthTokenException exception) {
            // Geçersiz cookie tarayıcıda kalmasın.
            refreshTokenCookieService.clearToken(httpResponse);
            throw exception;
        }
    }

    // Access token süresi dolmuş olabilir; bu yüzden refresh token ile çalışır.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody(required = false) LogoutRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (refreshTokenCookieService.isMobileClient(httpRequest)) {
            authService.logout(request != null ? request.refreshToken() : null);
            return;
        }

        requestOriginValidator.requireAllowedOrigin(httpRequest);

        authService.logout(refreshTokenCookieService.readToken(httpRequest).orElse(null));
        refreshTokenCookieService.clearToken(httpResponse);
    }

    // Hesap var/yok fark etmeksizin hep 202: e-posta adresinin kayıtlı olup olmadığı sızdırılmaz.
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request,
            HttpServletRequest httpRequest
    ) {
        passwordResetService.requestReset(request.email(), clientIpResolver.resolve(httpRequest));
    }

    // Başarılı reset tüm oturumları kapatır; bu tarayıcıdaki refresh cookie'si de silinir.
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request,
            HttpServletResponse httpResponse
    ) {
        passwordResetService.resetPassword(
                request.token(),
                request.newPassword(),
                request.confirmPassword()
        );
        refreshTokenCookieService.clearToken(httpResponse);
    }

    private AuthResponseDto deliver(
            AuthResponseDto response,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (refreshTokenCookieService.isMobileClient(httpRequest)) {
            return response;
        }

        refreshTokenCookieService.writeToken(
                httpResponse,
                response.refreshToken(),
                response.refreshTokenExpiresAt()
        );

        return response.withoutRefreshToken();
    }

    private ClientInfo clientInfo(HttpServletRequest request) {
        return new ClientInfo(
                null,
                refreshTokenCookieService.isMobileClient(request)
                        ? ClientPlatform.UNKNOWN
                        : ClientPlatform.WEB,
                clientIpResolver.resolve(request),
                request.getHeader(HttpHeaders.USER_AGENT)
        );
    }
}
