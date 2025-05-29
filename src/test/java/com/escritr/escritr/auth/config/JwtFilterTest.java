package com.escritr.escritr.auth.config;

import com.escritr.escritr.auth.controller.DTOs.DecodedToken;
import com.escritr.escritr.auth.service.TokenService;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private TokenService tokenService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private User sampleUser;
    private String validToken;
    private DecodedToken validDecodedToken;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = new User("testuser", "test@example.com", "password","name");
        sampleUser.setId(userId);
        sampleUser.setTokenVersion(1);

        validToken = "valid-jwt-token";
        validDecodedToken = new DecodedToken(userId, 1, "testuser", "test@example.com");

        SecurityContextHolder.clearContext();
    }

    // Clear context after each test to ensure isolation
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal should set SecurityContext when token is valid and user exists with matching version")
    void doFilterInternal_Success_ValidTokenAndUser() throws ServletException, IOException {

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.decodeToken(validToken)).thenReturn(validDecodedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should not be null in SecurityContext");
        assertTrue(authentication.isAuthenticated(), "Authentication should be marked as authenticated");

        Object principal = authentication.getPrincipal();
        assertInstanceOf(UserDetailsImpl.class, principal, "Principal should be UserDetailsImpl");
        assertEquals(sampleUser.getUsername(), ((UserDetailsImpl) principal).getUsername());
        assertEquals(sampleUser.getId(), ((UserDetailsImpl) principal).getId());

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should clear context and proceed if Authorization header is missing")
    void doFilterInternal_NoAuthHeader() throws ServletException, IOException {

        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null when no token is provided");
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(tokenService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("doFilterInternal should clear context and proceed if Authorization header doesn't start with Bearer")
    void doFilterInternal_InvalidAuthHeaderFormat() throws ServletException, IOException {

        when(request.getHeader("Authorization")).thenReturn("Basic somecredentials");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(tokenService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("doFilterInternal should clear context and proceed if token decoding fails")
    void doFilterInternal_TokenDecodingFails() throws ServletException, IOException {

        String invalidToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenService.decodeToken(invalidToken))
                .thenThrow(new AuthenticationTokenException("Invalid token", null, null));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null if token decoding fails");
        verify(filterChain, times(1)).doFilter(request, response);
        verify(userRepository, never()).findById(any(UUID.class)); // Ensure user lookup wasn't attempted
    }

    @Test
    @DisplayName("doFilterInternal should clear context and proceed if user is not found")
    void doFilterInternal_UserNotFound() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.decodeToken(validToken)).thenReturn(validDecodedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null if user is not found");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should clear context and proceed if token version mismatches")
    void doFilterInternal_TokenVersionMismatch() throws ServletException, IOException {
        User userWithDifferentVersion = new User("testuser", "test@example.com", "password","name");
        userWithDifferentVersion.setId(userId);
        userWithDifferentVersion.setTokenVersion(2); // User in DB has version 2

        DecodedToken decodedTokenWithOldVersion = new DecodedToken(userId, 1, "testuser", "test@example.com"); // Token has version 1

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.decodeToken(validToken)).thenReturn(decodedTokenWithOldVersion); // TokenService returns decoded token with version 1
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithDifferentVersion)); // Repository returns user with version 2

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null if token versions mismatch");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should clear context and proceed if unexpected error occurs during processing")
    void doFilterInternal_UnexpectedError() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.decodeToken(validToken)).thenReturn(validDecodedToken);
        when(userRepository.findById(userId)).thenThrow(new RuntimeException("Database connection failed"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be null on unexpected error");
        verify(filterChain, times(1)).doFilter(request, response);
    }
}