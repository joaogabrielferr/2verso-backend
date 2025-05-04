package com.escritr.escritr.auth;

import com.escritr.escritr.auth.DTOs.LoginDTO;
import com.escritr.escritr.auth.DTOs.LoginResponseDTO;
import com.escritr.escritr.auth.DTOs.RegisterDTO;
import com.escritr.escritr.auth.jwt.TokenService;
import com.escritr.escritr.auth.refresh_token.RefreshToken;
import com.escritr.escritr.auth.refresh_token.RefreshTokenRepository;
import com.escritr.escritr.auth.security.UserDetailsImpl;
import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.common.ErrorMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

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
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            TokenService tokenService,
            RefreshTokenRepository refreshTokenRepository // Add RefreshTokenRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.refreshTokenRepository = refreshTokenRepository; // Initialize
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO data, HttpServletResponse response) {

        try {
            var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
            Authentication auth = this.authenticationManager.authenticate(userNamePassword);
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userDetails.getUser();

            String accessToken = this.tokenService.generateAccessToken(user);

            RefreshToken refreshToken = this.tokenService.createRefreshToken(user);

            // Create HttpOnly Cookie for Refresh Token
            ResponseCookie refreshTokenCookie = ResponseCookie.from(refreshTokenCookieName, refreshToken.getToken())
                    .httpOnly(cookieHttpOnly)
                    .secure(cookieSecure) // Should be true in production (HTTPS)
                    .path(cookiePath) // Important: restrict path!
                    .maxAge(refreshTokenExpirationDays * 24 * 60 * 60) // Max age in seconds
                    .sameSite(cookieSameSite) // "Strict" or "Lax"
                    .build();

            // Return Access Token in Body, Refresh Token in Cookie
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(new LoginResponseDTO(accessToken));

        } catch (AuthenticationException ex) {
            System.err.println("Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Invalid credentials", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INVALID_CREDENTIALS));
        } catch (Exception ex) {
            // Catch other potential errors during token generation/cookie creation
            System.err.println("Login error: " + ex.getMessage());
            //TODO: replace with more robust logging
            ex.printStackTrace(); // Log stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage("Login failed due to an internal error.", ErrorAssetEnum.AUTHENTICATION,null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. Extract refresh token from HttpOnly cookie
        Cookie cookie = WebUtils.getCookie(request, refreshTokenCookieName);
        if (cookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Refresh token cookie not found.", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.NO_REFRESH_TOKEN));
        }
        String requestRefreshToken = cookie.getValue();

        try {
            // 2. Find and verify the refresh token in the database
            Optional<RefreshToken> refreshTokenOptional = tokenService.findByToken(requestRefreshToken);

            if (refreshTokenOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Invalid Refresh token.", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.INVALID_REFRESH_TOKEN));
            }

            RefreshToken refreshToken = tokenService.verifyRefreshToken(refreshTokenOptional.get()); // Checks expiry, throws if expired

            // 3. Generate a new access token
            User user = refreshToken.getUser();

            // Ensurse the user's token version hasn't changed
            // This handles cases where the user logged out elsewhere or password changed
            Optional<User> currentUserOpt = userRepository.findById(user.getId());
            if(currentUserOpt.isEmpty() || currentUserOpt.get().getTokenVersion() != user.getTokenVersion()) {
                // Token version mismatch or user deleted - invalidate the refresh token
                tokenService.deleteByToken(requestRefreshToken);
                // Clear the cookie on the client side
                ResponseCookie deleteCookie = ResponseCookie.from(refreshTokenCookieName, "")
                        .httpOnly(cookieHttpOnly).secure(cookieSecure).path(cookiePath)
                        .maxAge(0).sameSite(cookieSameSite).build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                        .body(new ErrorMessage("Session invalidated. Please log in again.", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.SESSION_INVALIDATED));
            }


            String newAccessToken = tokenService.generateAccessToken(user);

            return ResponseEntity.ok(new LoginResponseDTO(newAccessToken));

        } catch (RuntimeException ex) {
            System.err.println("Refresh token validation failed: " + ex.getMessage());

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