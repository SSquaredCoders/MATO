package com.lshzzz.mato.utils.users.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.utils.users.AuthTokenPolicy;
import com.lshzzz.mato.utils.users.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void expiredAccessSlidesRefreshSessionByThirtyDays() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtFilter filter = new JwtFilter(jwtUtil, refreshTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired-access");

        when(jwtUtil.getUsername("expired-access"))
            .thenThrow(new IllegalStateException("expired"));
        when(jwtUtil.extractUsernameFromExpiredToken("expired-access"))
            .thenReturn("user@example.com");
        when(refreshTokenService.getRefreshToken("user@example.com"))
            .thenReturn("stored-refresh");
        when(jwtUtil.isExpired("stored-refresh")).thenReturn(false);
        when(jwtUtil.getCategory("stored-refresh"))
            .thenReturn(AuthTokenPolicy.REFRESH_CATEGORY);
        when(jwtUtil.getRole("stored-refresh")).thenReturn("ROLE_USER");
        when(jwtUtil.getNickname("stored-refresh")).thenReturn("nickname");
        when(jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            "user@example.com",
            "nickname",
            "ROLE_USER",
            AuthTokenPolicy.ACCESS_TOKEN_TTL
        )).thenReturn("new-access");
        when(jwtUtil.createJwt(
            AuthTokenPolicy.REFRESH_CATEGORY,
            "user@example.com",
            "nickname",
            "ROLE_USER",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        )).thenReturn("new-refresh");
        when(jwtUtil.getUsername("new-access")).thenReturn("user@example.com");
        when(jwtUtil.getRole("new-access")).thenReturn("ROLE_USER");
        when(jwtUtil.getNickname("new-access")).thenReturn("nickname");

        filter.doFilterInternal(request, response, filterChain);

        verify(refreshTokenService).deleteRefreshToken("user@example.com");
        verify(refreshTokenService).saveRefreshToken(
            "user@example.com",
            "new-refresh",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );
        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("Authorization")).isEqualTo("Bearer new-access");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .isEqualTo("user@example.com");
    }
}
