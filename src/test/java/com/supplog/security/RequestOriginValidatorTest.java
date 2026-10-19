package com.supplog.security;

import com.supplog.exception.OriginNotAllowedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestOriginValidatorTest {

    private final RequestOriginValidator validator =
            new RequestOriginValidator("http://localhost:5173, https://supplog.vercel.app/");

    @Test
    void acceptsConfiguredOrigins() {
        assertDoesNotThrow(() -> validator.requireAllowedOrigin(withOrigin("https://supplog.vercel.app")));
        assertDoesNotThrow(() -> validator.requireAllowedOrigin(withOrigin("http://localhost:5173")));
    }

    @Test
    void rejectsMissingOrForeignOrigin() {
        assertThrows(OriginNotAllowedException.class,
                () -> validator.requireAllowedOrigin(new MockHttpServletRequest()));
        assertThrows(OriginNotAllowedException.class,
                () -> validator.requireAllowedOrigin(withOrigin("https://evil.example")));
        assertThrows(OriginNotAllowedException.class,
                () -> validator.requireAllowedOrigin(withOrigin("https://supplog.vercel.app.evil.example")));
    }

    private static MockHttpServletRequest withOrigin(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", origin);
        return request;
    }
}
