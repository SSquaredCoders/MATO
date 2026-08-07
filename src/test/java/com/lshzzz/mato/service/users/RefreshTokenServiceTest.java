package com.lshzzz.mato.service.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lshzzz.mato.utils.users.AuthTokenPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RefreshTokenServiceTest {

    @Test
    void storesRefreshTokenWithThirtyDayRedisTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        service.saveRefreshToken(
            "user@example.com",
            "refresh-token",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );

        verify(valueOperations).set(
            "refresh:user@example.com",
            "refresh-token",
            Duration.ofDays(30)
        );
    }

    @Test
    void redisFallbackExpiresAtTheSameThirtyDayBoundary() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("redis unavailable"))
            .when(valueOperations)
            .set(
                "refresh:user@example.com",
                "refresh-token",
                AuthTokenPolicy.REFRESH_TOKEN_TTL
            );
        when(valueOperations.get("refresh:user@example.com"))
            .thenThrow(new IllegalStateException("redis unavailable"));
        MutableClock clock = new MutableClock(Instant.parse("2026-08-07T00:00:00Z"));
        RefreshTokenService service = new RefreshTokenService(redisTemplate, clock);

        service.saveRefreshToken(
            "user@example.com",
            "refresh-token",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );
        clock.advance(Duration.ofDays(30).minusSeconds(1));
        assertThat(service.getRefreshToken("user@example.com")).isEqualTo("refresh-token");

        clock.advance(Duration.ofSeconds(1));
        assertThat(service.getRefreshToken("user@example.com")).isNull();
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
