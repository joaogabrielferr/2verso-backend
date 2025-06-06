package com.escritr.escritr.auth.controller;

import com.escritr.escritr.auth.controller.DTOs.*;
import com.escritr.escritr.auth.controller.mappers.UserMapper;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.auth.service.AuthenticationService;
import com.escritr.escritr.auth.service.TokenService;
import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;
import com.escritr.escritr.common.helpers.ErrorMessage;
import com.escritr.escritr.exceptions.InvalidRefreshTokenException;
import com.escritr.escritr.exceptions.SessionInvalidatedException;
import com.escritr.escritr.user.domain.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import java.util.HashMap;
import java.util.Map;


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

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshTokenCookieName, authResult.refreshTokenValue())
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty() && !"localhost".equalsIgnoreCase(cookieDomain)) {
            cookieBuilder.domain(cookieDomain); // Dynamically set
        }

        ResponseCookie refreshTokenCookie = cookieBuilder.build();

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(new ErrorMessage(ex.getMessage(), ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.INVALID_REFRESH_TOKEN));
        } catch (Exception ex) {
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

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body("Logout successful");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO data) {

        this.authenticationService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email){

        Boolean result = authenticationService.checkEmailAvailability(email);
        System.out.println("check email result for " + email + ":" + result.toString());
        Map<String,Boolean> response = new HashMap<>();
        response.put("available",result);
        return ResponseEntity.ok(response);

    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username){

        Boolean result = authenticationService.checkUsernameAvailability(username);
        Map<String,Boolean> response = new HashMap<>();
        response.put("available",result);
        return ResponseEntity.ok(response);

    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorMessage("No authenticated user found.", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.UNAUTHORIZED));
        }
        User currentUser = userDetails.getUser();
        UserLoginDTO userDto = userMapper.userToUserUserLoginDTO(currentUser);

        Map<String, UserLoginDTO> response = new HashMap<>();
        response.put("user", userDto);
        return ResponseEntity.ok(response);
    }



}