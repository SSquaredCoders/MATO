package com.lshzzz.mato.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    @Conditional(GoogleOAuthEnabledCondition.class)
    public ClientRegistrationRepository clientRegistrationRepository(
        @Value("${GOOGLE_CLIENT_ID}") String clientId,
        @Value("${GOOGLE_CLIENT_SECRET}") String clientSecret,
        @Value("${GOOGLE_REDIRECT_URI:{baseUrl}/login/oauth2/code/{registrationId}}")
        String redirectUri
    ) {
        ClientRegistration googleClientRegistration = CommonOAuth2Provider.GOOGLE
            .getBuilder("google")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .scope("openid", "profile", "email")
            .redirectUri(redirectUri)
            .build();

        return new InMemoryClientRegistrationRepository(googleClientRegistration);
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    public OAuth2AuthorizedClientService authorizedClientService(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
}
