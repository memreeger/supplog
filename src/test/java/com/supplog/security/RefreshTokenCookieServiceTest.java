package com.supplog.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenCookieServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");

    private final RefreshTokenCookieService service = new RefreshTokenCookieService(
            "X-Client-Type",
            "supplog_refresh",
            "/api/v1/auth",
            true,
            "Strict",
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void writesHardenedCookieWithRemainingLifetime() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.writeToken(response, "token-value", NOW.plus(Duration.ofDays(30)));

        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.startsWith("supplog_refresh=token-value"));
        assertTrue(header.contains("Path=/api/v1/auth"));
        assertTrue(header.contains("Max-Age=2592000"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
    }

    @Test
    void clearExpiresCookieImmediately() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearToken(response);

        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.startsWith("supplog_refresh=;"));
        assertTrue(header.contains("Max-Age=0"));
    }

    @Test
    void readsOnlyConfiguredNonBlankCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertTrue(service.readToken(request).isEmpty());

        request.setCookies(new Cookie("other", "x"), new Cookie("supplog_refresh", "abc"));
        assertEquals("abc", service.readToken(request).orElseThrow());

        request.setCookies(new Cookie("supplog_refresh", " "));
        assertTrue(service.readToken(request).isEmpty());
    }

    @Test
    void detectsMobileClientCaseInsensitively() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertFalse(service.isMobileClient(request));

        request.addHeader("X-Client-Type", "Mobile");
        assertTrue(service.isMobileClient(request));
    }
}
