package com.lshzzz.mato.utils.users.filter;

import com.lshzzz.mato.model.users.CustomUserDetails;
import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.utils.users.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain)
        throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.replace("Bearer ", "");
            String username = null;

            try {
                // Access Token에서 사용자 이름 추출
                username = jwtUtil.getUsername(accessToken);

                if (!jwtUtil.isExpired(accessToken)) {
                    setAuthentication(accessToken, request);
                } else {
                    throw new RuntimeException("Access Token expired");
                }

            } catch (Exception e) {
                // Access Token이 만료된 경우 -> Refresh Token 사용
                if (username == null && e.getCause() != null) {
                    username = jwtUtil.extractUsernameFromExpiredToken(accessToken);
                }

                if (username != null) {
                    String storedRefreshToken = refreshTokenService.getRefreshToken(username);

                    if (storedRefreshToken != null && !jwtUtil.isExpired(storedRefreshToken)
                        && Objects.equals("refresh", jwtUtil.getCategory(storedRefreshToken))) {

                        // 새 토큰 생성
                        String role = jwtUtil.getRole(storedRefreshToken);
                        String nickname = jwtUtil.getNickname(storedRefreshToken);
                        String newAccessToken = jwtUtil.createJwt("access", username, role, 600000L); // 10분
                        String newRefreshToken = jwtUtil.createJwt("refresh", username, role, 86400000L); // 1일

                        // Refresh 토큰 재발급 (Rotate)
                        refreshTokenService.deleteRefreshToken(username);
                        refreshTokenService.saveRefreshToken(username, newRefreshToken, 86400L);

                        // 새 Access Token 헤더에 추가
                        response.setHeader("Authorization", "Bearer " + newAccessToken);

                        // 인증 정보 설정
                        setAuthentication(newAccessToken, request);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid or expired refresh token");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String token, HttpServletRequest request) {
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);
        String nickname = jwtUtil.getNickname(token);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        CustomUserDetails userDetails = new CustomUserDetails(username, nickname, authorities);

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}