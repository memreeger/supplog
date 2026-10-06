package com.supplog.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void allowsConfiguredFrontendOriginAndPreflightMethod() {
        CorsConfig config = new CorsConfig("http://localhost:5173, https://app.example.com");
        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login")
        );

        assertEquals(
                java.util.List.of("http://localhost:5173", "https://app.example.com"),
                cors.getAllowedOrigins()
        );
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
    }
}
