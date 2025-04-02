package com.lshzzz.mato.controller.users;

import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.utils.users.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ReissueController {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    // JWT 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization").replace("Bearer ", "");
        String email;
        try {
            email = jwtUtil.getUsername(accessToken);
        } catch (ExpiredJwtException e) {
            return new ResponseEntity<>("Access token expired. Please log in again.",
                HttpStatus.UNAUTHORIZED);
        }

        String refresh = refreshTokenService.getRefreshToken(email);

        // Refresh 토큰 확인
        if (refresh == null) {
            return new ResponseEntity<>("Refresh token not found", HttpStatus.BAD_REQUEST);
        }

        // 만료 여부 확인
        if (jwtUtil.isExpired(refresh)) {
            return new ResponseEntity<>("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        // 토큰이 refresh인지 확인
        if (!"refresh".equals(jwtUtil.getCategory(refresh))) {
            return new ResponseEntity<>("Invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        // 새로운 Access Token 및 Refresh Token 생성
        String role = jwtUtil.getRole(refresh);
        String newAccessToken = jwtUtil.createJwt("access", email, role, 600000L);
        String newRefreshToken = jwtUtil.createJwt("refresh", email, role, 86400000L);

        // Redis에 기존의 Refresh 토큰 삭제 후 새 Refresh 토큰 저장
        refreshTokenService.deleteRefreshToken(email);
        refreshTokenService.saveRefreshToken(email, newRefreshToken, 86400L);

        // 새로운 Access Token을 헤더에 추가
        response.setHeader("Authorization", "Bearer " + newAccessToken);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}