package com.lshzzz.mato.utils.users;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${spring.jwt.secret-key}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
            Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
            .get("category", String.class);
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
            .get("username", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
            .get("role", String.class);
    }

    public String getNickname(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
            .get("nickname", String.class);
    }

    public Boolean isExpired(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
                .getExpiration().before(new Date());
        } catch (ExpiredJwtException exception) {
            return true;
        }
    }

    public String createJwt(
        String category,
        String username,
        String nickname,
        String role,
        Duration ttl
    ) {
        requirePositiveTtl(ttl);
        Instant issuedAt = Instant.now();

        return Jwts.builder()
            .claim("category", category)
            .claim("username", username)
            .claim("nickname", nickname)
            .claim("role", role)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plus(ttl)))
            .signWith(secretKey)
            .compact();
    }

    public String createJwt(String category, String username, String role, Duration ttl) {
        return createJwt(category, username, username, role, ttl);
    }

    public String extractUsernameFromExpiredToken(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
                .get("username", String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get("username", String.class);
        }
    }

    private void requirePositiveTtl(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }
}
