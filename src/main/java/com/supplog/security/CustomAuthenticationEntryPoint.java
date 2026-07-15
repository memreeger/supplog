package com.supplog.security;

import com.supplog.exception.ErrorResponse;
import com.supplog.exception.MessageHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final MessageHelper messageHelper;

    public CustomAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            MessageHelper messageHelper
    ) {
        this.objectMapper = objectMapper;
        this.messageHelper = messageHelper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        writeUnauthorizedResponse(
                response,
                "AUTHENTICATION_REQUIRED",
                "auth.authentication.required"
        );
    }

    public void writeUnauthorizedResponse(
            HttpServletResponse response,
            String code,
            String messageKey
    ) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                code,
                messageHelper.getMessage(messageKey),
                LocalDateTime.now()
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}