package com.supplog.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

// Web istemcide refresh token httpOnly cookie ile taşınır; mobil istemci body kullanır.
@Component
public class RefreshTokenCookieService {

    private static final String MOBILE_CLIENT = "mobile";

    private final String clientTypeHeader;
    private final String cookieName;
    private final String cookiePath;
    private final boolean secure;
    private final String sameSite;
    private final Clock clock;

    @Autowired
    public RefreshTokenCookieService(
            @Value("${app.auth.client-type-header:X-Client-Type}") String clientTypeHeader,
            @Value("${app.auth.refresh-cookie.name:supplog_refresh}") String cookieName,
            @Value("${app.auth.refresh-cookie.path:/api/v1/auth}") String cookiePath,
            @Value("${app.auth.refresh-cookie.secure:true}") boolean secure,
            @Value("${app.auth.refresh-cookie.same-site:Strict}") String sameSite
    ) {
        this(clientTypeHeader, cookieName, cookiePath, secure, sameSite, Clock.systemUTC());
    }

    RefreshTokenCookieService(
            String clientTypeHeader,
            String cookieName,
            String cookiePath,
            boolean secure,
            String sameSite,
            Clock clock
    ) {
        this.clientTypeHeader = clientTypeHeader;
        this.cookieName = cookieName;
        this.cookiePath = cookiePath;
        this.secure = secure;
        this.sameSite = sameSite;
        this.clock = clock;
    }

    public boolean isMobileClient(HttpServletRequest request) {
        return MOBILE_CLIENT.equalsIgnoreCase(request.getHeader(clientTypeHeader));
    }

    public Optional<String> readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    public void writeToken(HttpServletResponse response, String token, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);

        addCookie(response, token, maxAge.isNegative() ? Duration.ZERO : maxAge);
    }

    public void clearToken(HttpServletResponse response) {
        addCookie(response, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
