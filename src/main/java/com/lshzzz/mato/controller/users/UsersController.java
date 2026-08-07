package com.lshzzz.mato.controller.users;

import com.lshzzz.mato.exception.CustomException;
import com.lshzzz.mato.exception.ErrorCode;
import com.lshzzz.mato.model.users.CustomUserDetails;
import com.lshzzz.mato.model.users.dto.UsersCheckUserIdResponse;
import com.lshzzz.mato.model.users.dto.UsersLoginRequest;
import com.lshzzz.mato.model.users.dto.UsersLoginResponse;
import com.lshzzz.mato.model.users.dto.UsersRegisterRequest;
import com.lshzzz.mato.model.users.dto.UsersRegisterResponse;
import com.lshzzz.mato.model.users.dto.UsersUpdateRequest;
import com.lshzzz.mato.model.users.dto.UsersUpdateResponse;
import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.service.users.UsersService;
import com.lshzzz.mato.utils.users.AuthTokenPolicy;
import com.lshzzz.mato.utils.users.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<UsersLoginResponse> login(
        @Valid @RequestBody UsersLoginRequest request,
        HttpServletResponse response
    ) {
        UsersLoginResponse loginResponse = usersService.login(request.userId(), request.password());

        String accessToken = jwtUtil.createJwt(
            AuthTokenPolicy.ACCESS_CATEGORY,
            loginResponse.userId(),
            loginResponse.nickname(),
            loginResponse.role().name(),
            AuthTokenPolicy.ACCESS_TOKEN_TTL
        );
        String refreshToken = jwtUtil.createJwt(
            AuthTokenPolicy.REFRESH_CATEGORY,
            loginResponse.userId(),
            loginResponse.nickname(),
            loginResponse.role().name(),
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );

        refreshTokenService.saveRefreshToken(
            loginResponse.userId(),
            refreshToken,
            AuthTokenPolicy.REFRESH_TOKEN_TTL
        );
        response.setHeader("Authorization", "Bearer " + accessToken);

        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UsersLoginResponse> getCurrentUser() {
        return ResponseEntity.ok(usersService.getCurrentUser(getAuthenticatedUserId()));
    }

    @GetMapping("/check-userId")
    public ResponseEntity<UsersCheckUserIdResponse> checkUserId(@RequestParam String userId) {
        boolean isAvailable = usersService.isUserIdAvailable(userId);
        return ResponseEntity.ok(new UsersCheckUserIdResponse(isAvailable));
    }

    @PostMapping("/register")
    public ResponseEntity<UsersRegisterResponse> register(
        @Valid @RequestBody UsersRegisterRequest request
    ) {
        return ResponseEntity.ok(usersService.register(request));
    }

    @PutMapping("/update")
    public ResponseEntity<UsersUpdateResponse> updateUser(
        @Valid @RequestBody UsersUpdateRequest request
    ) {
        UsersUpdateResponse response = usersService.updateUser(getAuthenticatedUserId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser() {
        String userId = getAuthenticatedUserId();
        usersService.deleteUser(userId);
        refreshTokenService.deleteRefreshToken(userId);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        String userId = getAuthenticatedUserId();
        refreshTokenService.deleteRefreshToken(userId);

        Map<String, String> payload = new HashMap<>();
        payload.put("message", "로그아웃이 완료되었습니다.");
        return ResponseEntity.ok(payload);
    }

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return userDetails.getUsername();
    }
}
