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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.replace("Bearer ", "");
            String username = null;

            try {
                username = jwtUtil.getUsername(accessToken);

                if (!jwtUtil.isExpired(accessToken)) {
                    setAuthentication(accessToken, request);
                } else {
                    throw new RuntimeException("Access token expired");
                }
            } catch (Exception exception) {
                if (username == null) {
                    username = jwtUtil.extractUsernameFromExpiredToken(accessToken);
                }

                if (username != null) {
                    String storedRefreshToken = refreshTokenService.getRefreshToken(username);

                    if (storedRefreshToken != null
                        && !jwtUtil.isExpired(storedRefreshToken)
                        && Objects.equals("refresh", jwtUtil.getCategory(storedRefreshToken))) {
                        String role = jwtUtil.getRole(storedRefreshToken);
                        String nickname = resolveNickname(storedRefreshToken, username);
                        String newAccessToken = jwtUtil.createJwt(
                            "access",
                            username,
                            nickname,
                            role,
                            600000L
                        );
                        String newRefreshToken = jwtUtil.createJwt(
                            "refresh",
                            username,
                            nickname,
                            role,
                            86400000L
                        );

                        refreshTokenService.deleteRefreshToken(username);
                        refreshTokenService.saveRefreshToken(username, newRefreshToken, 86400L);
                        response.setHeader("Authorization", "Bearer " + newAccessToken);
                        setAuthentication(newAccessToken, request);
                    } else {
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid or expired refresh token"
                        );
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
        String nickname = resolveNickname(token, username);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        CustomUserDetails userDetails = new CustomUserDetails(username, nickname, authorities);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String resolveNickname(String token, String fallbackUsername) {
        String nickname = jwtUtil.getNickname(token);
        if (nickname == null || nickname.isBlank()) {
            return fallbackUsername;
        }
        return nickname;
    }
}
