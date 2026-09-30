package com.supplog.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Window> requests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowSeconds;
    private final int supportMaxRequests;
    private final long supportWindowSeconds;

    public AuthRateLimitFilter(
            @Value("${app.rate-limit.auth.max-requests:20}") int maxRequests,
            @Value("${app.rate-limit.auth.window-seconds:60}") long windowSeconds,
            @Value("${app.rate-limit.support.max-requests:120}") int supportMaxRequests,
            @Value("${app.rate-limit.support.window-seconds:60}") long supportWindowSeconds
    ) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.supportMaxRequests = supportMaxRequests;
        this.supportWindowSeconds = supportWindowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/v1/auth/")
                && !path.startsWith("/api/v1/support");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        boolean supportRequest = request.getServletPath().startsWith("/api/v1/support");
        int requestLimit = supportRequest ? supportMaxRequests : maxRequests;
        long requestWindowSeconds = supportRequest ? supportWindowSeconds : windowSeconds;
        String clientKey = request.getRemoteAddr() + (supportRequest ? ":support" : ":auth");

        Window window = requests.compute(clientKey, (key, current) ->
                current == null || now - current.startedAt() >= requestWindowSeconds
                        ? new Window(now, 1)
                        : new Window(current.startedAt(), current.count() + 1)
        );

        if (window.count() > requestLimit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", Long.toString(requestWindowSeconds));
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}"
            );
            return;
        }

        if (requests.size() > 10_000) {
            requests.entrySet().removeIf(entry ->
                    now - entry.getValue().startedAt()
                            >= Math.max(windowSeconds, supportWindowSeconds)
            );
        }

        filterChain.doFilter(request, response);
    }

    private record Window(long startedAt, int count) {
    }
}
