package com.escritr.escritr.auth.config;
import com.escritr.escritr.auth.controller.DTOs.DecodedToken;
import com.escritr.escritr.auth.service.TokenService;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component // Make sure it's a Spring component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final TokenService tokenService; // Inject these
    private final UserRepository userRepository;

    public JwtFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    private String getTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = this.getTokenFromHeader(request);

        if (token != null) {
            try {
                DecodedToken decodedJWT = tokenService.decodeToken(token); // This might throw AuthenticationTokenException

                Optional<User> userOpt = userRepository.findById(decodedJWT.userId());

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getTokenVersion() == decodedJWT.tokenVersion()) {
                        UserDetailsImpl userDetails = new UserDetailsImpl(user);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        logger.debug("Authentication successful for user: {}", user.getUsername());
                    } else {
                        logger.warn("Token version mismatch for user: {}. Clearing context.", user.getUsername());
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    logger.warn("User ID from token not found: {}. Clearing context.", decodedJWT.userId());
                    SecurityContextHolder.clearContext();
                }
            } catch (AuthenticationTokenException e) {
                logger.warn("JWT Authentication failed: {}. Clearing context.", e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                // Catching general exceptions during token processing
                logger.error("Unexpected error during JWT processing: {}. Clearing context.", e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }
        } else {
            logger.trace("No JWT token found in request header for path: {}", request.getRequestURI());
            // No token, so no authentication to set. SecurityContext remains empty.
            SecurityContextHolder.clearContext();
        }

        // ALWAYS continue the filter chain.
        // Spring Security will decide if the now (potentially unauthenticated) request
        // can access the endpoint based on permitAll() or authenticated().
        filterChain.doFilter(request, response);
    }
}
