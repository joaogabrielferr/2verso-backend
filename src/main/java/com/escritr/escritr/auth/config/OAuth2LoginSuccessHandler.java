package com.escritr.escritr.auth.config;

import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.auth.service.TokenService;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);


    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Value("${frontend.oauth2.redirect-url}")
    private String frontendRedirectUrl;

    @Value("${api.security.token.refresh-cookie-name:refreshToken}")
    private String refreshTokenCookieName;
    @Value("${api.security.token.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;
    @Value("${api.security.token.cookie-secure}")
    private boolean cookieSecure;
    @Value("${api.security.token.cookie-http-only}")
    private boolean cookieHttpOnly;
    @Value("${api.security.token.cookie-path:/}")
    private String cookiePath;
    @Value("${api.security.token.cookie-same-site:Lax}")
    private String cookieSameSite;
    @Value("${api.security.token.domain.value:}")
    private String cookieDomain;

    public OAuth2LoginSuccessHandler(TokenService tokenService,UserRepository userRepository){
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        logger.info("OAuth2LoginSuccessHandler --- Authentication object type: {}", authentication.getClass().getName());
        logger.info("OAuth2LoginSuccessHandler --- Principal object type: {}", authentication.getPrincipal().getClass().getName());
        logger.info("OAuth2LoginSuccessHandler --- Principal object toString: {}", authentication.getPrincipal().toString());
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Invalidate old sessions and generate new tokens
        user.incrementTokenVersion(); // Invalidate tokens from previous sessions
        userRepository.save(user);
        // TODO: TokenService.createRefreshToken may need to save the user if token version changed.

        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.createRefreshToken(user);

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshTokenCookieName, refreshToken.getToken())
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .sameSite(cookieSameSite);

        if (StringUtils.hasText(cookieDomain) && !"localhost".equalsIgnoreCase(cookieDomain)) {
            cookieBuilder.domain(cookieDomain);
        }
        response.addHeader("Set-Cookie", cookieBuilder.build().toString());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", accessToken)
                // .queryParam("userId", user.getId().toString()) // Optional: send user info
                // .queryParam("username", user.getUsername())
                .build().toUriString();

        logger.info("OAuth2 login successful for user: {}. Redirecting to frontend.", user.getUsername());
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}