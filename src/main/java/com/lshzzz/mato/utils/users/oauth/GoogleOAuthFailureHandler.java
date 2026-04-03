package com.lshzzz.mato.utils.users.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${FRONTEND_BASE_URL:http://127.0.0.1:4173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        String normalizedBaseUrl = frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;
        String encodedError = URLEncoder.encode(
            exception.getMessage() == null ? "google_login_failed" : exception.getMessage(),
            StandardCharsets.UTF_8
        );
        response.sendRedirect(normalizedBaseUrl + "/auth/google/callback#error=" + encodedError);
    }
}
