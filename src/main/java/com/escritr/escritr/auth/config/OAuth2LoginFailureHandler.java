package com.escritr.escritr.auth.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${frontend.oauth2.error-redirect-url:${frontend.oauth2.redirect-url:/}}")
    private String frontendErrorRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        logger.warn("OAuth2 Authentication Failed: {}", exception.getMessage(), exception);

        String errorMessage = "Login with provider failed. Please try again or use a different method.";
        if (exception.getMessage().contains("An account already exists")) {
            errorMessage = exception.getMessage();
        } else if (exception.getMessage().contains("did not return required user information") || exception.getMessage().contains("did not return a user ID")) {
            errorMessage = "Could not retrieve required information from the login provider. Please try again.";
        }

        String targetUrl = UriComponentsBuilder.fromUriString(frontendErrorRedirectUrl)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();

        setDefaultFailureUrl(targetUrl);
        super.onAuthenticationFailure(request, response, exception);
    }
}