/*package com.supplog.filter;

import com.supplog.service.user.impl.CustomUserDetailsService;
import com.supplog.service.user.impl.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final int tokenBeginIndexNumber = 7;

    private final String AUTHORIZATION = "Authorization";
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(tokenBeginIndexNumber);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);

        UserDetails user = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);

    }
}
*/
/*
package com.supplog.filter;

import com.supplog.security.CustomAuthenticationEntryPoint;
import com.supplog.service.user.impl.CustomUserDetailsService;
import com.supplog.service.user.impl.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(TOKEN_BEGIN_INDEX);

        if (!jwtService.isValid(token)) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "Invalid or expired JWT token"
                    )
            );

            return;
        }

        try {
            String username =
                    jwtService.extractUsername(token);

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails user =
                        userDetailsService
                                .loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                user.getAuthorities()
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();

            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "JWT authentication failed",
                            exception
                    )
            );
        }
    }
}
*/

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

            String username = claims.getSubject();
            Object tokenVersionValue = claims.get("tokenVersion");

            if (username == null || username.isBlank()) {
                writeTokenInvalidResponse(response);
                return;
            }

            if (!(tokenVersionValue instanceof Number tokenVersion)) {
                writeTokenInvalidResponse(response);
                return;
            }

            CustomUserDetails userDetails =
                    (CustomUserDetails) userDetailsService
                            .loadUserByUsername(username);

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