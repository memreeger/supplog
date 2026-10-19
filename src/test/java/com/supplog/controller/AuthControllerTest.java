package com.supplog.controller;

import com.supplog.dto.login.AuthResponseDto;
import com.supplog.exception.AuthTokenException;
import com.supplog.exception.GlobalExceptionHandler;
import com.supplog.exception.MessageHelper;
import com.supplog.security.ClientIpResolver;
import com.supplog.security.RefreshTokenCookieService;
import com.supplog.security.RequestOriginValidator;
import com.supplog.service.admin.audit.AdminAuditService;
import com.supplog.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private static final String ORIGIN = "https://supplog.vercel.app";
    private static final String LOGIN_BODY = """
            {"username":"tester","password":"Test-pass-123"}
            """;

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);

        AuthController controller = new AuthController(
                authService,
                new RefreshTokenCookieService(
                        "X-Client-Type", "supplog_refresh", "/api/v1/auth", true, "Strict"
                ),
                new RequestOriginValidator(ORIGIN),
                new ClientIpResolver(0)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(
                        mock(MessageHelper.class),
                        mock(AdminAuditService.class)
                ))
                .build();
    }

    @Test
    void webLoginSetsHttpOnlyCookieAndHidesTokenInBody() throws Exception {
        when(authService.login(any(), any())).thenReturn(response("refresh-1"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("supplog_refresh=refresh-1")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void mobileLoginReturnsTokenInBodyWithoutCookie() throws Exception {
        when(authService.login(any(), any())).thenReturn(response("refresh-1"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client-Type", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-1"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void webRefreshRequiresAllowedOrigin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("supplog_refresh", "refresh-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGIN_NOT_ALLOWED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "https://evil.example")
                        .cookie(new Cookie("supplog_refresh", "refresh-1")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
    }

    @Test
    void webRefreshReadsCookieAndRotatesIt() throws Exception {
        when(authService.refresh(eq("refresh-1"), any())).thenReturn(response("refresh-2"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", ORIGIN)
                        .cookie(new Cookie("supplog_refresh", "refresh-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("supplog_refresh=refresh-2")));
    }

    @Test
    void webRefreshIgnoresBodyToken() throws Exception {
        when(authService.refresh(isNull(), any()))
                .thenThrow(new AuthTokenException("REFRESH_TOKEN_INVALID", "auth.refresh.invalid"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"stolen\"}"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).refresh(eq("stolen"), any());
    }

    @Test
    void failedWebRefreshClearsCookie() throws Exception {
        when(authService.refresh(eq("refresh-1"), any()))
                .thenThrow(new AuthTokenException("REFRESH_TOKEN_REUSED", "auth.refresh.reused"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", ORIGIN)
                        .cookie(new Cookie("supplog_refresh", "refresh-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void mobileRefreshUsesBodyAndNeverCookie() throws Exception {
        when(authService.refresh(eq("body-token"), any())).thenReturn(response("refresh-2"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Type", "mobile")
                        .cookie(new Cookie("supplog_refresh", "cookie-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"body-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-2"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        verify(authService, never()).refresh(eq("cookie-token"), any());
    }

    @Test
    void webLogoutRevokesCookieTokenAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", ORIGIN)
                        .cookie(new Cookie("supplog_refresh", "refresh-1")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("refresh-1");
    }

    private static AuthResponseDto response(String refreshToken) {
        return new AuthResponseDto(
                1L,
                "tester",
                "tester@example.test",
                "access",
                "Bearer",
                false,
                refreshToken,
                Instant.now().plusSeconds(900),
                Instant.now().plusSeconds(86_400)
        );
    }
}
