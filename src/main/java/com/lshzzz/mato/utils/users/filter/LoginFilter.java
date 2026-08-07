package com.lshzzz.mato.utils.users.filter;

import com.lshzzz.mato.model.users.CustomUserDetails;
import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.utils.users.AuthTokenPolicy;
import com.lshzzz.mato.utils.users.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
        HttpServletResponse response) throws AuthenticationException {

        //클라이언트 요청에서 username, password 추출
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        //스프링 시큐리티에서 username과 password를 검증하기 위해서는 token에 담아야 함
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            username, password, null);

        //token에 담은 검증을 위한 AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
    }

    //로그인 성공시 실행하는 메소드
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain, Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String access = jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            customUserDetails.getUsername(),
            customUserDetails.getNickname(),
            customUserDetails.getAuthorities().iterator().next().getAuthority(),
            AuthTokenPolicy.ACCESS_TOKEN_TTL
        );
        String refresh = jwtUtil.createJwt(
            AuthTokenPolicy.REFRESH_CATEGORY,
            customUserDetails.getUsername(),
            customUserDetails.getNickname(),
            customUserDetails.getAuthorities().iterator().next().getAuthority(),
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );

        refreshTokenService.saveRefreshToken(
            customUserDetails.getUsername(),
            refresh,
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );

        response.setHeader("Authorization", "Bearer " + access);
        response.setStatus(HttpStatus.OK.value());
    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
        HttpServletResponse response, AuthenticationException failed) throws IOException {
        response.setContentType("application/json");
        response.setStatus(401);
        response.getWriter().write("{\"error\":\"Invalid username or password\"}");
        response.getWriter().flush();
    }
}
