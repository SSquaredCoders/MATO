package com.lshzzz.mato.service.users;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final ConcurrentMap<String, LocalRefreshToken> localFallback =
        new ConcurrentHashMap<>();

    @Autowired
    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RefreshTokenService(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public void saveRefreshToken(String username, String refreshToken, Duration ttl) {
        requirePositiveTtl(ttl);
        String key = key(username);
        try {
            redisTemplate.opsForValue().set(key, refreshToken, ttl);
            localFallback.remove(key);
        } catch (Exception exception) {
            localFallback.put(
                key,
                new LocalRefreshToken(refreshToken, clock.instant().plus(ttl))
            );
        }
    }

    public String getRefreshToken(String username) {
        String key = key(username);
        try {
            String token = redisTemplate.opsForValue().get(key);
            return token != null ? token : getLocalRefreshToken(key);
        } catch (Exception exception) {
            return getLocalRefreshToken(key);
        }
    }

    public void deleteRefreshToken(String username) {
        String key = key(username);
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            // Local development can continue with the in-memory fallback.
        }
        localFallback.remove(key);
    }

    private String key(String username) {
        return "refresh:" + username;
    }

    private String getLocalRefreshToken(String key) {
        LocalRefreshToken token = localFallback.get(key);
        if (token == null) {
            return null;
        }
        if (!token.expiresAt().isAfter(clock.instant())) {
            localFallback.remove(key, token);
            return null;
        }
        return token.value();
    }

    private void requirePositiveTtl(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }

    private record LocalRefreshToken(String value, Instant expiresAt) {
    }
}
