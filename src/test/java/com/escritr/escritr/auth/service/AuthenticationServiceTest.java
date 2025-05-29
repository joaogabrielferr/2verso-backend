package com.escritr.escritr.auth.service;

import com.escritr.escritr.auth.controller.DTOs.AuthenticationResult;
import com.escritr.escritr.auth.controller.DTOs.LoginDTO;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.exceptions.InvalidRefreshTokenException;
import com.escritr.escritr.exceptions.SessionInvalidatedException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserRepository userRepository;

    // Inject mocks into this instance
    @InjectMocks
    private AuthenticationService authenticationService;

    private User sampleUser;
    private LoginDTO sampleLoginDTO;
    private RefreshToken sampleRefreshToken;
    private Authentication successfulAuthentication;

    @BeforeEach
    void setUp() {
        sampleUser = new User("testuser", "test@example.com", "encodedPassword","name");
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setTokenVersion(1);

        UserDetailsImpl sampleUserDetails = new UserDetailsImpl(sampleUser);

        sampleLoginDTO = new LoginDTO("testuser", "password");

        successfulAuthentication = new UsernamePasswordAuthenticationToken(
                sampleUserDetails,
                null,
                sampleUserDetails.getAuthorities()
        );


        sampleRefreshToken = new RefreshToken();
        sampleRefreshToken.setId(UUID.randomUUID());
        sampleRefreshToken.setUser(sampleUser);
        sampleRefreshToken.setToken("valid-refresh-token");
        sampleRefreshToken.setExpiryDate(Instant.now().plusSeconds(3600)); // Valid expiry
    }


    @Test
    @DisplayName("authenticateAndGenerateTokens should return tokens on successful authentication")
    void authenticateAndGenerateTokens_Success() {

        when(authenticationManager.authenticate(
                argThat(token -> token.getName().equals(sampleLoginDTO.login()) &&
                        token.getCredentials().equals(sampleLoginDTO.password()))
        )).thenReturn(successfulAuthentication);


        when(tokenService.generateAccessToken(sampleUser)).thenReturn("mockAccessToken");

        when(tokenService.createRefreshToken(sampleUser)).thenReturn(sampleRefreshToken);

        AuthenticationResult result = authenticationService.authenticateAndGenerateTokens(sampleLoginDTO);

        assertNotNull(result);
        assertEquals("mockAccessToken", result.accessToken());
        assertEquals(sampleRefreshToken.getToken(), result.refreshTokenValue());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, times(1)).generateAccessToken(sampleUser);
        verify(tokenService, times(1)).createRefreshToken(sampleUser);
    }

    @Test
    @DisplayName("authenticateAndGenerateTokens should throw AuthenticationException on failed authentication")
    void authenticateAndGenerateTokens_Failure_BadCredentials() {

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials")); // Simulate auth failure

        BadCredentialsException thrown = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticateAndGenerateTokens(sampleLoginDTO)
        );

        assertEquals("Invalid credentials", thrown.getMessage());

        // Verify AuthenticationManager was called, but token generation was not
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService, never()).generateAccessToken(any(User.class));
        verify(tokenService, never()).createRefreshToken(any(User.class));
    }


    @Test
    @DisplayName("updateAcessTokenWithRefreshToken should return new access token for valid refresh token and matching version")
    void updateAccessTokenWithRefreshToken_Success() {
        String requestRefreshToken = "valid-refresh-token";
        User currentUserState = new User("testuser", "test@example.com", "encodedPassword","name");
        currentUserState.setId(sampleUser.getId());
        currentUserState.setTokenVersion(1); // Same version as in refresh token's user

        when(tokenService.findByToken(requestRefreshToken)).thenReturn(Optional.of(sampleRefreshToken));
        when(tokenService.verifyRefreshToken(sampleRefreshToken)).thenReturn(sampleRefreshToken);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(currentUserState));
        when(tokenService.generateAccessToken(currentUserState)).thenReturn("newMockAccessToken");

        AuthenticationResult result = authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken);

        assertNotNull(result);
        assertEquals("newMockAccessToken", result.accessToken());
        assertNull(result.refreshTokenValue(), "Refresh token value should be null in the response for refresh");

        verify(tokenService, times(1)).findByToken(requestRefreshToken);
        verify(tokenService, times(1)).verifyRefreshToken(sampleRefreshToken);
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(tokenService, times(1)).generateAccessToken(currentUserState);
        verify(tokenService, never()).deleteByToken(anyString());
    }

    @Test
    @DisplayName("updateAcessTokenWithRefreshToken should throw InvalidRefreshTokenException if token not found")
    void updateAccessTokenWithRefreshToken_Failure_TokenNotFound() {
        String requestRefreshToken = "invalid-refresh-token";

        when(tokenService.findByToken(requestRefreshToken)).thenReturn(Optional.empty());

        InvalidRefreshTokenException thrown = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken)
        );
        assertEquals("Invalid Refresh token.", thrown.getMessage());

        verify(tokenService, times(1)).findByToken(requestRefreshToken);
        verify(tokenService, never()).verifyRefreshToken(any(RefreshToken.class));
        verify(userRepository, never()).findById(any(UUID.class));
        verify(tokenService, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("updateAcessTokenWithRefreshToken should propagate exception from verifyRefreshToken")
    void updateAccessTokenWithRefreshToken_Failure_TokenExpired() {

        String requestRefreshToken = "expired-refresh-token";
        sampleRefreshToken.setExpiryDate(Instant.now().minusSeconds(10)); // Make it expired

        when(tokenService.findByToken(requestRefreshToken)).thenReturn(Optional.of(sampleRefreshToken));

        when(tokenService.verifyRefreshToken(sampleRefreshToken))
                .thenThrow(new InvalidRefreshTokenException("Refresh token was expired."));

        InvalidRefreshTokenException thrown = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken)
        );
        assertEquals("Refresh token was expired.", thrown.getMessage());

        verify(tokenService, times(1)).findByToken(requestRefreshToken);
        verify(tokenService, times(1)).verifyRefreshToken(sampleRefreshToken);
        verify(userRepository, never()).findById(any(UUID.class));
        verify(tokenService, never()).generateAccessToken(any(User.class));
    }


    @Test
    @DisplayName("updateAcessTokenWithRefreshToken should throw SessionInvalidatedException if user not found")
    void updateAccessTokenWithRefreshToken_Failure_UserNotFound() {
        String requestRefreshToken = "valid-refresh-token";

        when(tokenService.findByToken(requestRefreshToken)).thenReturn(Optional.of(sampleRefreshToken));
        when(tokenService.verifyRefreshToken(sampleRefreshToken)).thenReturn(sampleRefreshToken);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());

        SessionInvalidatedException thrown = assertThrows(
                SessionInvalidatedException.class,
                () -> authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken)
        );
        assertEquals("User not found.", thrown.getMessage());

        verify(tokenService, times(1)).findByToken(requestRefreshToken);
        verify(tokenService, times(1)).verifyRefreshToken(sampleRefreshToken);
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(tokenService, never()).generateAccessToken(any(User.class));
        verify(tokenService, never()).deleteByToken(anyString());
    }


    @Test
    @DisplayName("updateAcessTokenWithRefreshToken should throw SessionInvalidatedException and delete token if token versions mismatch")
    void updateAccessTokenWithRefreshToken_Failure_VersionMismatch() {
        String requestRefreshToken = "valid-refresh-token";
        User currentUserState = new User("testuser", "test@example.com", "encodedPassword","name");
        currentUserState.setId(sampleUser.getId());
        currentUserState.setTokenVersion(2);

        // Arrange
        when(tokenService.findByToken(requestRefreshToken)).thenReturn(Optional.of(sampleRefreshToken)); // sampleRefreshToken user version is 1
        when(tokenService.verifyRefreshToken(sampleRefreshToken)).thenReturn(sampleRefreshToken);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(currentUserState));

        SessionInvalidatedException thrown = assertThrows(
                SessionInvalidatedException.class,
                () -> authenticationService.updateAcessTokenWithRefreshToken(requestRefreshToken)
        );

        verify(tokenService, times(1)).findByToken(requestRefreshToken);
        verify(tokenService, times(1)).verifyRefreshToken(sampleRefreshToken);
        verify(userRepository, times(1)).findById(sampleUser.getId());
        verify(tokenService, times(1)).deleteByToken(requestRefreshToken);
        verify(tokenService, never()).generateAccessToken(any(User.class));
    }
}