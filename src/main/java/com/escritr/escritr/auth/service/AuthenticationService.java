package com.escritr.escritr.auth.service;

import com.escritr.escritr.auth.controller.DTOs.AuthenticationResult;
import com.escritr.escritr.auth.controller.DTOs.LoginDTO;
import com.escritr.escritr.auth.controller.DTOs.LoginResponseDTO;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.auth.repository.RefreshTokenRepository;
import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.common.ErrorMessage;
import com.escritr.escritr.exceptions.AuthenticationTokenException;
import com.escritr.escritr.exceptions.InvalidRefreshTokenException;
import com.escritr.escritr.exceptions.SessionInvalidatedException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthenticationService {


    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AuthenticationService(AuthenticationManager authenticationManager, TokenService tokenService,UserRepository userRepository){
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userRepository = userRepository;

    }

    @Transactional
    public AuthenticationResult authenticateAndGenerateTokens(LoginDTO loginData) throws AuthenticationException {

        Authentication authentication = authenticateUser(loginData.login(), loginData.password());
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.createRefreshToken(user);

        return new AuthenticationResult(accessToken, refreshToken.getToken());

    }


    private Authentication authenticateUser(String username, String password) throws AuthenticationException {
        var usernamePasswordAuthToken = new UsernamePasswordAuthenticationToken(username, password);
        // AuthenticationManager throws AuthenticationException if authentication fails
        return authenticationManager.authenticate(usernamePasswordAuthToken);
    }

    public AuthenticationResult updateAcessTokenWithRefreshToken(String requestRefreshToken){

        RefreshToken refreshToken = tokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid Refresh token."));

        refreshToken = tokenService.verifyRefreshToken(refreshToken);

        User user = refreshToken.getUser();
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new SessionInvalidatedException("User not found."));

        if (currentUser.getTokenVersion() != user.getTokenVersion()) {
            tokenService.deleteByToken(requestRefreshToken);
            throw new SessionInvalidatedException();
        }

        String newAcessToken = tokenService.generateAccessToken(currentUser);
        return new AuthenticationResult(newAcessToken,null);
    }

}
