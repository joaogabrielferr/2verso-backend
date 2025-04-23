package com.escritr.escritr.auth.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.escritr.escritr.auth.DTOs.DecodedToken;
import com.escritr.escritr.auth.model.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user){
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("escritr")
                    .withSubject(user.getId().toString())
                    .withClaim("usr", user.getUsername())
                    .withClaim("eml", user.getEmail())
                    .withClaim("ver", user.getTokenVersion()) // for future revocation
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        }catch(JWTCreationException ex){
            throw new RuntimeException("Error while authenticating");
        }
    }

//    public String validateToken(String token){
//        try{
//            Algorithm algorithm = Algorithm.HMAC256(secret);
//            return JWT.require(algorithm).withIssuer("escritr")
//                    .build()
//                    .verify(token)
//                    .getSubject();
//        }catch(JWTVerificationException ex){
//            return null;
//        }
//    }

    private Instant generateExpirationDate(){
        return LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.of("-03:00"));
    }

    public DecodedToken decodeToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var jwt = JWT.require(algorithm)
                    .withIssuer("escritr")
                    .build()
                    .verify(token);

            return new DecodedToken(
                    UUID.fromString(jwt.getSubject()),
                    jwt.getClaim("ver").asInt(),
                    jwt.getClaim("usr").asString(),
                    jwt.getClaim("eml").asString()
            );
        } catch (JWTVerificationException ex) {
            return null;
        }
    }

}
