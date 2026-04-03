package com.lshzzz.mato.service.users;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentMap<String, String> localFallback = new ConcurrentHashMap<>();

    public void saveRefreshToken(String username, String refreshToken, long duration) {
        String key = key(username);
        try {
            redisTemplate.opsForValue().set(key, refreshToken, duration, TimeUnit.SECONDS);
        } catch (Exception exception) {
            localFallback.put(key, refreshToken);
        }
    }

    public String getRefreshToken(String username) {
        String key = key(username);
        try {
            String token = redisTemplate.opsForValue().get(key);
            return token != null ? token : localFallback.get(key);
        } catch (Exception exception) {
            return localFallback.get(key);
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
}
