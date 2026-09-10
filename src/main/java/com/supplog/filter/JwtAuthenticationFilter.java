package com.supplog.filter;

import com.supplog.security.CustomAuthenticationEntryPoint;
import com.supplog.service.user.impl.CustomUserDetails;
import com.supplog.service.user.impl.CustomUserDetailsService;
import com.supplog.service.user.impl.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final int TOKEN_BEGIN_INDEX = 7;
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            CustomAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(AUTHORIZATION);

        // Token verilmemişse anonymous olarak devam et.
        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Header var fakat Bearer formatında değil.
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            writeTokenInvalidResponse(response);
            return;
        }

        String token = authorizationHeader
                .substring(TOKEN_BEGIN_INDEX)
                .trim();

        if (token.isBlank()) {
            writeTokenInvalidResponse(response);
            return;
        }

        try {

            Claims claims = jwtService.parseToken(token);

            String subject = claims.getSubject();

            Object tokenVersionValue =
                    claims.get("tokenVersion");

            Object tokenSchemaVersionValue =
                    claims.get("tokenSchemaVersion");

            if (subject == null || subject.isBlank()) {
                writeTokenInvalidResponse(response);
                return;
            }

            if (!(tokenVersionValue instanceof Number tokenVersion)) {
                writeTokenInvalidResponse(response);
                return;
            }

            if (!(tokenSchemaVersionValue instanceof Number tokenSchemaVersion)) {
                writeTokenInvalidResponse(response);
                return;
            }

            if (tokenSchemaVersion.intValue()
                    != JwtService.TOKEN_SCHEMA_VERSION) {

                writeTokenInvalidResponse(response);
                return;
            }

            Long userId = Long.valueOf(subject);

            CustomUserDetails userDetails =
                    userDetailsService.loadUserById(userId);

            if (tokenVersion.intValue()
                    != userDetails.getTokenVersion()) {

                SecurityContextHolder.clearContext();

                authenticationEntryPoint.writeUnauthorizedResponse(
                        response,
                        "TOKEN_REVOKED",
                        "auth.token.revoked"
                );

                return;
            }

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (ExpiredJwtException exception) {

            SecurityContextHolder.clearContext();

            authenticationEntryPoint.writeUnauthorizedResponse(
                    response,
                    "TOKEN_EXPIRED",
                    "auth.token.expired"
            );

            return;

        } catch (
                SignatureException
                | MalformedJwtException
                | UnsupportedJwtException
                | IllegalArgumentException exception
        ) {

            SecurityContextHolder.clearContext();
            writeTokenInvalidResponse(response);
            return;

        } catch (UsernameNotFoundException exception) {

            SecurityContextHolder.clearContext();

            authenticationEntryPoint.writeUnauthorizedResponse(
                    response,
                    "AUTHENTICATION_REQUIRED",
                    "auth.authentication.required"
            );

            return;

        } catch (JwtException exception) {

            SecurityContextHolder.clearContext();
            writeTokenInvalidResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeTokenInvalidResponse(
            HttpServletResponse response
    ) throws IOException {

        authenticationEntryPoint.writeUnauthorizedResponse(
                response,
                "TOKEN_INVALID",
                "auth.token.invalid"
        );
    }
}