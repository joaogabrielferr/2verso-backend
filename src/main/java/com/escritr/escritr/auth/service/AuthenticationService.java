package com.escritr.escritr.auth.service;

import com.escritr.escritr.auth.controller.DTOs.AuthenticationResult;
import com.escritr.escritr.auth.controller.DTOs.LoginDTO;
import com.escritr.escritr.auth.controller.DTOs.RegisterDTO;
import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;
import com.escritr.escritr.exceptions.InvalidRefreshTokenException;
import com.escritr.escritr.exceptions.SessionInvalidatedException;
import com.escritr.escritr.exceptions.UserAlreadyExistsException;
import com.escritr.escritr.exceptions.WrongParameterException;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        //User user = this.userRepository.findByEmailOrUsername(loginData.login(),loginData.login());

        return new AuthenticationResult(accessToken, refreshToken.getToken(),user);

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

        //lazy fetch
        User user = refreshToken.getUser();

        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new SessionInvalidatedException("User not found."));

        if (currentUser.getTokenVersion() != user.getTokenVersion()) {
            tokenService.deleteByToken(requestRefreshToken);
            throw new SessionInvalidatedException();
        }

        String newAcessToken = tokenService.generateAccessToken(currentUser);
        return new AuthenticationResult(newAcessToken,null,user);
    }

    public void register(RegisterDTO data){
        if (this.userRepository.findByEmail(data.email()).isPresent()) {
            throw new UserAlreadyExistsException("The e-mail is already linked to an existing account");
        }

        if(this.userRepository.findByUsername(data.username()).isPresent()){
            throw new UserAlreadyExistsException("The username is already linked to an existing account");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User user = new User(data.username(), data.email(), encryptedPassword, data.name());
        this.userRepository.save(user);
    }

    public Boolean checkEmailAvailability(String email){
        if(email == null){
            throw new WrongParameterException("e-mail can't be empty", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }
        if(email.isBlank()){
            throw new WrongParameterException("e-mail can't be empty", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }

        return this.userRepository.findByEmail(email).isEmpty();
    }

    public Boolean checkUsernameAvailability(String username){
        if(username == null){
            throw new WrongParameterException("username can't be empty", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }
        if(username.isBlank()){
            throw new WrongParameterException("username can't be empty", ErrorAssetEnum.AUTHENTICATION, ErrorCodeEnum.INPUT_FORMAT_ERROR);
        }

        return this.userRepository.findByUsername(username).isEmpty();
    }




}
