package com.escritr.escritr.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.escritr.escritr.auth.controller.DTOs.DecodedToken;
import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;
import com.escritr.escritr.exceptions.InvalidRefreshTokenException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.repository.RefreshTokenRepository;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Optional;

@Service // Add @Service annotation
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${api.security.token.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    public String generateAccessToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("escritr")
                    .withSubject(user.getId().toString())
                    .withClaim("usr", user.getUsername())
                    .withClaim("eml", user.getEmail())
                    .withClaim("ver", user.getTokenVersion())
                    .withExpiresAt(generateAccessTokenExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            //TODO: Log the exception properly
            throw new AuthenticationTokenException("Error while generating access token", ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional // Ensure atomicity when saving
    public RefreshToken createRefreshToken(User user) {

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(generateRefreshTokenExpirationDate());
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyRefreshToken(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            // TODO: throw specific exception here
            throw new InvalidRefreshTokenException("Refresh token " + token.getToken() + " was expired. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteAllUserRefreshTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }


    private Instant generateAccessTokenExpirationDate() {
        return Instant.now().plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
    }

    private Instant generateRefreshTokenExpirationDate() {
        return Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);
    }

    public DecodedToken decodeToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var jwt = JWT.require(algorithm)
                    .withIssuer("escritr")
                    .build()
                    .verify(token);

            // Check if required claims exist before accessing
            Integer tokenVersion = jwt.getClaim("ver").asInt();
            String username = jwt.getClaim("usr").asString();
            String email = jwt.getClaim("eml").asString();
            UUID userId = UUID.fromString(jwt.getSubject());
            System.out.println(tokenVersion);
            if (tokenVersion == null || username == null || email == null) {
                System.err.println("Token missing required claims.");
                throw  new AuthenticationTokenException("Token missing required claims.", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INVALID_TOKEN);
            }


            return new DecodedToken(
                    userId,
                    tokenVersion,
                    username,
                    email
            );
        } catch (JWTVerificationException ex) {
            // Log the verification failure
            System.err.println("Access Token Verification Failed: " + ex.getMessage());
            throw new AuthenticationTokenException("Invalid or expired token",ErrorAssetEnum.AUTHENTICATION,ErrorCodeEnum.INVALID_TOKEN);
        }catch (IllegalArgumentException e) { // Example: Catching specific expected exceptions
            System.err.println("Error decoding token (e.g., UUID parsing): " + e.getMessage());
            throw new AuthenticationTokenException("Malformed token data", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INVALID_TOKEN);
        }
    }

}