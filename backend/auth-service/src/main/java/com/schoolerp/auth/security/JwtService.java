package com.schoolerp.auth.security;

import com.schoolerp.common.security.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {
    private final SecretKey secretKey;
    private final long accessTokenTtlSeconds;

    public JwtService(@Value("${school-erp.security.jwt-secret}") String jwtSecret,
                      @Value("${school-erp.security.access-token-ttl-seconds}") long accessTokenTtlSeconds) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(CurrentUser currentUser) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(currentUser.username())
                .claim("userId", currentUser.userId())
                .claim("displayName", currentUser.displayName())
                .claim("userType", currentUser.userType())
                .claim("roles", currentUser.roles())
                .claim("orgUnit", currentUser.orgUnit())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public CurrentUser parseCurrentUser(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        List<String> roles = claims.get("roles", List.class);
        return new CurrentUser(
                claims.get("userId", Long.class),
                claims.getSubject(),
                claims.get("displayName", String.class),
                claims.get("userType", String.class),
                roles == null ? List.of() : roles,
                claims.get("orgUnit", String.class)
        );
    }
}
