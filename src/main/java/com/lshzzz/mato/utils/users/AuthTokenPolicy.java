package com.lshzzz.mato.utils.users;

import java.time.Duration;

/**
 * Shared lifetime and category policy for authentication tokens.
 */
public final class AuthTokenPolicy {

    public static final String ACCESS_CATEGORY = "access";
    public static final String REFRESH_CATEGORY = "refresh";
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(10);
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private AuthTokenPolicy() {
    }
}
