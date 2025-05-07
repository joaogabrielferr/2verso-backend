package com.escritr.escritr.auth.controller;

import com.escritr.escritr.auth.controller.DTOs.AuthenticationResult;
import com.escritr.escritr.auth.controller.DTOs.LoginDTO;
import com.escritr.escritr.auth.controller.DTOs.LoginResponseDTO;
import com.escritr.escritr.auth.controller.DTOs.RegisterDTO;
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
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final AuthenticationService authenticationService;

    // Read cookie properties from application.properties/yml for flexibility
    @Value("${api.security.token.refresh-cookie-name:refreshToken}")
    private String refreshTokenCookieName;

    @Value("${api.security.token.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Value("${api.security.token.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${api.security.token.cookie-http-only:true}")
    private boolean cookieHttpOnly;

    @Value("${api.security.token.cookie-path:/api/auth}")
    private String cookiePath;

    @Value("${api.security.token.cookie-same-site:Strict}")
    private String cookieSameSite;


    public AuthenticationController(
            UserRepository userRepository,
            TokenService tokenService,
            AuthenticationService authenticationService
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO data, HttpServletResponse response) {


            AuthenticationResult authResult = this.authenticationService.authenticateAndGenerateTokens(data);

        // Create HttpOnly Cookie for Refresh Token
            ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenCookieName, authResult.refreshTokenValue())
                    .httpOnly(cookieHttpOnly)
                    .secure(cookieSecure) // Should be true in production (HTTPS)
                    .path(cookiePath) // Important: restrict path!!
                    .maxAge(refreshTokenExpirationDays * 24 * 60 * 60) // Max age in seconds
                    .sameSite(cookieSameSite) // "Strict" or "Lax"
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(new LoginResponseDTO(authResult.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {

        Cookie cookie = WebUtils.getCookie(request, refreshTokenCookieName);
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Refresh token cookie not found.", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.NO_REFRESH_TOKEN));
        }
        String requestRefreshToken = cookie.getValue();

        try {

            AuthenticationResult result = authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken);

            return ResponseEntity.ok(new LoginResponseDTO(result.accessToken()));

        } catch (InvalidRefreshTokenException | SessionInvalidatedException ex) {
            System.err.println("Refresh token validation failed: " + ex.getMessage());
            // Clear the cookie
            ResponseCookie deleteCookie = ResponseCookie.from(refreshTokenCookieName, "")
                    .httpOnly(cookieHttpOnly).secure(cookieSecure).path(cookiePath)
                    .maxAge(0).sameSite(cookieSameSite).build();
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

        ResponseCookie deleteCookie = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("Logout successful");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO data) {

        if (this.userRepository.findByEmailOrUsername(data.email(), data.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage("There is already a user with that email or username.", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.USER_ALREADY_EXISTS));
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User user = new User(data.username(), data.email(), encryptedPassword);
        this.userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}