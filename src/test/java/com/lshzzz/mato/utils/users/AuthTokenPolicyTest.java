package com.lshzzz.mato.utils.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class AuthTokenPolicyTest {

    private static final String SECRET = "test-secret-key-test-secret-key-1234";

    @Test
    void keepsAccessAtTenMinutesAndRefreshAtThirtyDays() {
        assertThat(AuthTokenPolicy.ACCESS_TOKEN_TTL).isEqualTo(Duration.ofMinutes(10));
        assertThat(AuthTokenPolicy.REFRESH_TOKEN_TTL).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void jwtExpirationUsesThePolicyDuration() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);

        String accessToken = jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            "user@example.com",
            "nickname",
            "ROLE_USER",
            AuthTokenPolicy.ACCESS_TOKEN_TTL
        );
        String refreshToken = jwtUtil.createJwt(
            AuthTokenPolicy.REFRESH_CATEGORY,
            "user@example.com",
            "nickname",
            "ROLE_USER",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );

        assertThat(tokenLifetime(accessToken)).isEqualTo(AuthTokenPolicy.ACCESS_TOKEN_TTL);
        assertThat(tokenLifetime(refreshToken)).isEqualTo(AuthTokenPolicy.REFRESH_TOKEN_TTL);
    }

    @Test
    void rejectsNonPositiveTokenLifetime() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);

        assertThatThrownBy(() -> jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            "user@example.com",
            "ROLE_USER",
            Duration.ZERO
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identifiesExpiredJwtWithoutPropagatingParserException() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
            .claim("category", AuthTokenPolicy.REFRESH_CATEGORY)
            .claim("username", "user@example.com")
            .issuedAt(Date.from(now.minus(Duration.ofMinutes(2))))
            .expiration(Date.from(now.minus(Duration.ofMinutes(1))))
            .signWith(secretKey())
            .compact();

        assertThat(jwtUtil.isExpired(expiredToken)).isTrue();
    }

    private Duration tokenLifetime(String token) {
        var claims = Jwts.parser()
            .verifyWith(secretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return Duration.between(
            claims.getIssuedAt().toInstant(),
            claims.getExpiration().toInstant()
        );
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(
            SECRET.getBytes(StandardCharsets.UTF_8),
            Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }
}
