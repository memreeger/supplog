package com.supplog.service.user.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final int TOKEN_SCHEMA_VERSION = 2;

    private final SecretKey signKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationMs = expirationMs;
    }

    public String generateToken(
            Long userId,
            int tokenVersion
    ) {
        long currentTime = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tokenVersion", tokenVersion)
                .claim("tokenSchemaVersion",TOKEN_SCHEMA_VERSION)
                .issuedAt(new Date(currentTime))
                .expiration(new Date(currentTime + expirationMs))
                .signWith(signKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}