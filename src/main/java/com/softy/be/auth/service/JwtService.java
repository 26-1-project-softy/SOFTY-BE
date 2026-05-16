package com.softy.be.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds:86400}") long accessExpirationSeconds,
            @Value("${jwt.refresh-expiration-seconds:1209600}") long refreshExpirationSeconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String createAccessToken(Long userId, String name, String activeRole) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("name", name)
                .claim("activeRole", activeRole)
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken(Long userId, String activeRole) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("activeRole", activeRole)
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        return parseUserId(token);
    }

    public Long extractAccessUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            if (!"ACCESS".equals(tokenType)) {
                throw new IllegalStateException("인증에는 액세스 토큰만 사용할 수 있습니다.");
            }
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalStateException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }

    public String extractActiveRole(String token) {
        try {
            Claims claims = parseClaims(token);
            String activeRole = claims.get("activeRole", String.class);
            if (activeRole == null || activeRole.trim().isEmpty()) {
                return "";
            }
            return activeRole.trim();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalStateException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }

    private Long parseUserId(String token) {
        try {
            String subject = parseClaims(token).getSubject();
            return Long.parseLong(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalStateException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
