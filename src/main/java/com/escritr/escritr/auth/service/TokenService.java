package com.escritr.escritr.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.escritr.escritr.auth.controller.DTOs.DecodedToken;
import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.repository.RefreshTokenRepository;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            // TODO: throw specific exception here
            throw new RuntimeException("Refresh token " + token.getToken() + " was expired. Please log in again.");
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
        return LocalDateTime.now()

                .plusMinutes(accessTokenExpirationMinutes)
                .toInstant(ZoneOffset.of("-03:00"));
    }

    private Instant generateRefreshTokenExpirationDate() {
        return LocalDateTime.now()
                .plusDays(refreshTokenExpirationDays)
                .toInstant(ZoneOffset.of("-03:00"));
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
        } catch (Exception e) {
            // Catch other potential errors like UUID parsing
            System.err.println("Error decoding token: " + e.getMessage());
            return null;
        }
    }

}