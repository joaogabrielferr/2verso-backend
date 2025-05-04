package com.escritr.escritr.auth.jwt;

import com.escritr.escritr.auth.DTOs.DecodedToken;
import com.escritr.escritr.auth.User;
import com.escritr.escritr.auth.UserRepository;
import com.escritr.escritr.auth.security.SecurityConfig;
import com.escritr.escritr.auth.security.UserDetailsImpl;
import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.common.ErrorMessage;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;

    @Autowired
    UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = this.getTokenFromHeader(request);

        try {
            if (token != null) {
                DecodedToken decodedJWT = tokenService.decodeToken(token); // This might throw

                // No need to check if decodedJWT is null if decodeToken always throws on error
                Optional<User> userOpt = userRepository.findById(decodedJWT.userId());

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getTokenVersion() == decodedJWT.tokenVersion()) {
                        UserDetailsImpl userDetails = new UserDetailsImpl(user);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        System.out.println("Authentication successful for user: " + user.getUsername());
                    } else {
                        System.err.println("Token version mismatch for user: " + user.getUsername());
                        SecurityContextHolder.clearContext();
                        // Optionally throw an exception here too if you want the catch block to handle it
                        // throw new AuthenticationTokenException("Token version mismatch", ErrorCodeEnum.TOKEN_INVALID);
                    }
                } else {
                    System.err.println("User ID from token not found: " + decodedJWT.userId());
                    SecurityContextHolder.clearContext();
                    // Optionally throw an exception here too
                    // throw new AuthenticationTokenException("User not found", ErrorCodeEnum.USER_NOT_FOUND);
                }
            }
            // If token is null, or validation passed, continue the filter chain
            filterChain.doFilter(request, response);

        } catch (AuthenticationTokenException e) {
            // Token is invalid (expired, malformed, signature mismatch etc.)
            System.err.println("Authentication failed via JWT Filter: " + e.getMessage());
            SecurityContextHolder.clearContext();

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorMessage errorResponse = new ErrorMessage(
                    e.getMessage(),
                    ErrorAssetEnum.AUTHENTICATION,
                    ErrorCodeEnum.TOKEN_EXPIRED
            );

            objectMapper.writeValue(response.getWriter(), errorResponse);
            return;

        } catch (Exception e) {
            logger.error("Unexpected error in JWT Filter", e);
            SecurityContextHolder.clearContext();

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorMessage errorResponse = new ErrorMessage(
                    "Internal server error during authentication",
                    ErrorAssetEnum.AUTHENTICATION,
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR
            );
            objectMapper.writeValue(response.getWriter(), errorResponse);
            return;
        }
    }

    private String getTokenFromHeader(HttpServletRequest request){
        var authHeader = request.getHeader("Authorization");
        if(authHeader == null)return null;
        return authHeader.replace("Bearer ","");
    }



}
