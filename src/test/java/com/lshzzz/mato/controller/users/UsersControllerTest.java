package com.lshzzz.mato.controller.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lshzzz.mato.model.users.Role;
import com.lshzzz.mato.model.users.dto.UsersLoginRequest;
import com.lshzzz.mato.model.users.dto.UsersLoginResponse;
import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.service.users.UsersService;
import com.lshzzz.mato.utils.users.AuthTokenPolicy;
import com.lshzzz.mato.utils.users.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class UsersControllerTest {

    @Test
    void apiLoginUsesSharedTokenLifetimes() {
        UsersService usersService = mock(UsersService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        UsersController controller = new UsersController(
            usersService,
            jwtUtil,
            refreshTokenService
        );
        UsersLoginRequest request = new UsersLoginRequest("user@example.com", "password");
        UsersLoginResponse loginResponse = new UsersLoginResponse(
            1L,
            "user@example.com",
            "nickname",
            Role.USER
        );
        when(usersService.login(request.userId(), request.password())).thenReturn(loginResponse);
        when(jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            loginResponse.userId(),
            loginResponse.nickname(),
            loginResponse.role().name(),
            AuthTokenPolicy.ACCESS_TOKEN_TTL
        )).thenReturn("access-token");
        when(jwtUtil.createJwt(
            AuthTokenPolicy.REFRESH_CATEGORY,
            loginResponse.userId(),
            loginResponse.nickname(),
            loginResponse.role().name(),
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        )).thenReturn("refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.login(request, response);

        verify(refreshTokenService).saveRefreshToken(
            loginResponse.userId(),
            "refresh-token",
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );
        assertThat(response.getHeader("Authorization")).isEqualTo("Bearer access-token");
        assertThat(result.getBody()).isEqualTo(loginResponse);
    }
}
