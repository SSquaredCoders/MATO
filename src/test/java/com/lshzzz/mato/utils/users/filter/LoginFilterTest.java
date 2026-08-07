package com.lshzzz.mato.utils.users.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lshzzz.mato.model.users.CustomUserDetails;
import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.utils.users.AuthTokenPolicy;
import com.lshzzz.mato.utils.users.JwtUtil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class LoginFilterTest {

    @Test
    void loginUsesSharedTokenLifetimes() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails user = new CustomUserDetails(
            "user@example.com",
            "nickname",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            user.getUsername(),
            user.getNickname(),
            "ROLE_USER",
            AuthTokenPolicy.ACCESS_TOKEN_TTL
        )).thenReturn("access-token");
        when(jwtUtil.createJwt(
            AuthTokenPolicy.REFRESH_CATEGORY,
            user.getUsername(),
            user.getNickname(),
            "ROLE_USER",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        )).thenReturn("refresh-token");
        LoginFilter filter = new LoginFilter(
            authenticationManager,
            jwtUtil,
            refreshTokenService
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.successfulAuthentication(
            new MockHttpServletRequest(),
            response,
            mock(jakarta.servlet.FilterChain.class),
            authentication
        );

        verify(refreshTokenService).saveRefreshToken(
            user.getUsername(),
            "refresh-token",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );
        org.assertj.core.api.Assertions.assertThat(response.getHeader("Authorization"))
            .isEqualTo("Bearer access-token");
    }
}
