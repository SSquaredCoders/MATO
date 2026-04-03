package com.lshzzz.mato.utils.users.oauth;

import com.lshzzz.mato.model.users.dto.UsersLoginResponse;
import com.lshzzz.mato.service.users.RefreshTokenService;
import com.lshzzz.mato.service.users.UsersService;
import com.lshzzz.mato.utils.users.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsersService usersService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${FRONTEND_BASE_URL:http://127.0.0.1:4173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendRedirect(buildCallbackUrl(null, "unsupported_authentication"));
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        String subject = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (subject == null || subject.isBlank()) {
            response.sendRedirect(buildCallbackUrl(null, "missing_google_subject"));
            return;
        }

        UsersLoginResponse user = usersService.upsertGoogleUser(subject, name, email);
        String role = user.role().name();
        String accessToken = jwtUtil.createJwt(
            "access",
            user.userId(),
            user.nickname(),
            role,
            600000L
        );
        String refreshToken = jwtUtil.createJwt(
            "refresh",
            user.userId(),
            user.nickname(),
            role,
            86400000L
        );

        refreshTokenService.saveRefreshToken(user.userId(), refreshToken, 86400L);
        response.sendRedirect(buildCallbackUrl(accessToken, null));
    }

    private String buildCallbackUrl(String accessToken, String error) {
        String normalizedBaseUrl = frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;

        StringBuilder fragment = new StringBuilder();
        if (accessToken != null) {
            fragment.append("accessToken=")
                .append(URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
        }
        if (error != null) {
            if (!fragment.isEmpty()) {
                fragment.append("&");
            }
            fragment.append("error=")
                .append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        }

        return normalizedBaseUrl + "/auth/google/callback#" + fragment;
    }
}
