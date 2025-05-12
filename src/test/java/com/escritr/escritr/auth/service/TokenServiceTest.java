package com.escritr.escritr.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.escritr.escritr.auth.controller.DTOs.DecodedToken;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.repository.RefreshTokenRepository;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import com.escritr.escritr.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;


    @InjectMocks
    private TokenService tokenService;

    private User sampleUser;
    private final String testSecret = "test-secret-key-longer-than-256-bits-for-hs256-algorithm";
    private final long accessTokenExpMinutes = 15;


    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(tokenService, "secret", testSecret);
        ReflectionTestUtils.setField(tokenService, "accessTokenExpirationMinutes", accessTokenExpMinutes);
        long refreshTokenExpDays = 7;
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpirationDays", refreshTokenExpDays);

        sampleUser = new User("testuser", "test@example.com", "password");
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setTokenVersion(1);
    }

    @Test
    @DisplayName("generateAccessToken should return a non-empty token string for a valid user")
    void generateAccessToken_shouldReturnNonEmptyToken() {
        String token = tokenService.generateAccessToken(sampleUser);

        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");

        DecodedToken decoded = tokenService.decodeToken(token);
        assertEquals(sampleUser.getId(), decoded.userId());
        assertEquals(sampleUser.getUsername(), decoded.username());
        assertEquals(sampleUser.getEmail(), decoded.email());
        assertEquals(sampleUser.getTokenVersion(), decoded.tokenVersion());
    }

    @Test
    @DisplayName("createRefreshToken should generate, save and return a new refresh token")
    void createRefreshToken_shouldSaveAndReturnNewToken() {

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken rt = invocation.getArgument(0);
            rt.setId(UUID.randomUUID());
            return rt;
        });

        RefreshToken refreshToken = tokenService.createRefreshToken(sampleUser);

        assertNotNull(refreshToken, "RefreshToken should not be null");
        assertNotNull(refreshToken.getToken(), "RefreshToken string should not be null");
        assertNotNull(refreshToken.getExpiryDate(), "RefreshToken expiry date should not be null");
        assertEquals(sampleUser, refreshToken.getUser(), "RefreshToken should be linked to the correct user");

        verify(refreshTokenRepository, times(1)).deleteByUser(sampleUser);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyRefreshToken should return token if not expired")
    void verifyRefreshToken_shouldReturnToken_whenNotExpired() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));
        token.setUser(sampleUser);

        RefreshToken result = tokenService.verifyRefreshToken(token);

        assertEquals(token, result, "Should return the same token if valid");
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyRefreshToken should throw exception and delete token if expired")
    void verifyRefreshToken_shouldThrowAndDeletetoken_whenExpired() {
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken("expired-token");
        expiredToken.setExpiryDate(Instant.now().minus(1, ChronoUnit.DAYS));
        expiredToken.setUser(sampleUser);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            tokenService.verifyRefreshToken(expiredToken);
        });
        assertTrue(exception.getMessage().contains("was expired"), "Exception message should indicate expiry");

        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }

    @Test
    @DisplayName("decodeToken should return DecodedToken for a valid JWT")
    void decodeToken_shouldReturnDecodedToken_forValidJwt() {
        String jwtToken = JWT.create()
                .withIssuer("escritr")
                .withSubject(sampleUser.getId().toString())
                .withClaim("usr", sampleUser.getUsername())
                .withClaim("eml", sampleUser.getEmail())
                .withClaim("ver", sampleUser.getTokenVersion())
                .withExpiresAt(Instant.now().plusMillis(accessTokenExpMinutes * 60 * 1000))
                .sign(Algorithm.HMAC256(testSecret));

        DecodedToken decoded = tokenService.decodeToken(jwtToken);

        assertNotNull(decoded);
        assertEquals(sampleUser.getId(), decoded.userId());
        assertEquals(sampleUser.getUsername(), decoded.username());
        assertEquals(sampleUser.getEmail(), decoded.email());
        assertEquals(sampleUser.getTokenVersion(), decoded.tokenVersion());
    }

    @Test
    @DisplayName("decodeToken should throw AuthenticationTokenException for an invalid JWT")
    void decodeToken_shouldThrowException_forInvalidJwt() {
        String invalidJwtToken = "this.is.not.a.valid.jwt";

        AuthenticationTokenException exception = assertThrows(AuthenticationTokenException.class, () -> {
            tokenService.decodeToken(invalidJwtToken);
        });
        assertEquals("Invalid or expired token", exception.getMessage());
    }

    @Test
    @DisplayName("decodeToken should throw AuthenticationTokenException if token is missing claims")
    void decodeToken_shouldThrowException_whenTokenMissingClaims() {
        // Generate a token missing the 'ver' claim
        String jwtTokenMissingClaim = JWT.create()
                .withIssuer("escritr")
                .withSubject(sampleUser.getId().toString())
                .withClaim("usr", sampleUser.getUsername())
                .withClaim("eml", sampleUser.getEmail())
                .withExpiresAt(Instant.now().plusMillis(accessTokenExpMinutes * 60 * 1000))
                .sign(Algorithm.HMAC256(testSecret));

        AuthenticationTokenException exception = assertThrows(AuthenticationTokenException.class, () -> {
            tokenService.decodeToken(jwtTokenMissingClaim);
        });
        assertTrue(exception.getMessage().contains("Token missing required claims."), "Error message should indicate some claim is missing");
    }
}