package com.escritr.escritr.auth.controller;

import com.escritr.escritr.auth.controller.DTOs.*;
import com.escritr.escritr.auth.controller.mappers.UserMapper;
import com.escritr.escritr.auth.service.AuthenticationService;
import com.escritr.escritr.auth.service.TokenService;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.repository.RefreshTokenRepository;
import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.common.ErrorMessage;
import com.escritr.escritr.exceptions.InvalidRefreshTokenException;
import com.escritr.escritr.exceptions.SessionInvalidatedException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import java.util.Optional;


@RestController
@RequestMapping("api/auth")
public class AuthenticationController {

    private final TokenService tokenService;
    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;

    // Read cookie properties from application.properties/yml for flexibility
    @Value("${api.security.token.refresh-cookie-name:refreshToken}")
    private String refreshTokenCookieName;

    @Value("${api.security.token.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Value("${api.security.token.cookie-secure}")
    private boolean cookieSecure;

    @Value("${api.security.token.cookie-http-only}")
    private boolean cookieHttpOnly;

    @Value("${api.security.token.cookie-path}")
    private String cookiePath;

    @Value("${api.security.token.cookie-same-site}")
    private String cookieSameSite;

    @Value("${api.security.token.domain.value}")
    private String cookieDomain;


    public AuthenticationController(
            TokenService tokenService,
            AuthenticationService authenticationService,
            UserMapper userMapper
    ) {
        this.tokenService = tokenService;
        this.authenticationService = authenticationService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO data) {


        AuthenticationResult authResult = this.authenticationService.authenticateAndGenerateTokens(data);

        // Create HttpOnly Cookie for Refresh Token

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshTokenCookieName, authResult.refreshTokenValue())
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure) // Should be true in production (HTTPS)
                .path(cookiePath)
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty() && !"localhost".equalsIgnoreCase(cookieDomain)) {
            cookieBuilder.domain(cookieDomain); // Dynamically set
        }

        ResponseCookie refreshTokenCookie = cookieBuilder.build();


//            ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenCookieName, authResult.refreshTokenValue())
//                    .httpOnly(cookieHttpOnly)
//                    .secure(cookieSecure) // Should be true in production (HTTPS)
//                    .path(cookiePath)
//                    .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
//                    .sameSite(cookieSameSite);



        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new LoginResponseDTO(authResult.accessToken(),userMapper.userToUserUserLoginDTO(authResult.user())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {

        Cookie cookie = WebUtils.getCookie(request, refreshTokenCookieName);
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Refresh token cookie not found.", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.NO_REFRESH_TOKEN));
        }
        String requestRefreshToken = cookie.getValue();

        try {

            AuthenticationResult result = authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken);

            return ResponseEntity.ok(new LoginResponseDTO(result.accessToken(),userMapper.userToUserUserLoginDTO(result.user())));

        } catch (InvalidRefreshTokenException | SessionInvalidatedException ex) {
            System.err.println("Refresh token validation failed: " + ex.getMessage());
            // Clear the cookie
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshTokenCookieName, "")
                    .httpOnly(cookieHttpOnly)
                    .secure(cookieSecure)
                    .path(cookiePath)
                    .maxAge(0)
                    .sameSite(cookieSameSite);

            if (cookieDomain != null && !cookieDomain.isEmpty() && !"localhost".equalsIgnoreCase(cookieDomain)) {
                cookieBuilder.domain(cookieDomain);
            }

            ResponseCookie deleteCookie = cookieBuilder.build();
//            ResponseCookie deleteCookie = ResponseCookie.from(refreshTokenCookieName, "")
//                    .httpOnly(cookieHttpOnly).secure(cookieSecure).path(cookiePath)
//                    .maxAge(0).sameSite(cookieSameSite).build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(new ErrorMessage(ex.getMessage(), ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.INVALID_REFRESH_TOKEN));
        } catch (Exception ex) {
            System.err.println("Error during token refresh: " + ex.getMessage());
            //TODO: replace with more robust logging
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage("Token refresh failed.", ErrorAssetEnum.AUTHENTICATION,null));
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = WebUtils.getCookie(request, refreshTokenCookieName);
        if (cookie != null) {
            String token = cookie.getValue();
            tokenService.deleteByToken(token);
        }

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(0)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty() && !"localhost".equalsIgnoreCase(cookieDomain)) {
            cookieBuilder.domain(cookieDomain);
        }

        ResponseCookie deleteCookie = cookieBuilder.build();

//        ResponseCookie deleteCookie = ResponseCookie.from(refreshTokenCookieName, "")
//                .httpOnly(cookieHttpOnly)
//                .secure(cookieSecure)
//                .path(cookiePath)
//                .maxAge(0)
//                .sameSite(cookieSameSite)
//                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("Logout successful");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO data) {

        this.authenticationService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}