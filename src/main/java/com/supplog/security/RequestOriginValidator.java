package com.supplog.security;

import com.supplog.exception.OriginNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// Cookie ile kimlik taşıyan isteklerde CSRF'e karşı ikinci savunma (SameSite=Strict'e ek).
@Component
public class RequestOriginValidator {

    private final Set<String> allowedOrigins;

    public RequestOriginValidator(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(RequestOriginValidator::normalize)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void requireAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin == null || !allowedOrigins.contains(normalize(origin))) {
            throw new OriginNotAllowedException();
        }
    }

    private static String normalize(String origin) {
        String trimmed = origin.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
