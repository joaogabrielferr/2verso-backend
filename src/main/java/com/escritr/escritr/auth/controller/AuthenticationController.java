package com.escritr.escritr.auth.controller;

import com.escritr.escritr.auth.DTOs.LoginDTO;
import com.escritr.escritr.auth.DTOs.LoginResponseDTO;
import com.escritr.escritr.auth.DTOs.RegisterDTO;
import com.escritr.escritr.auth.jwt.TokenService;
import com.escritr.escritr.auth.model.User;
import com.escritr.escritr.auth.repository.UserRepository;
import com.escritr.escritr.auth.security.UserDetailsImpl;
import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorMessage;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {


    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthenticationController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            TokenService tokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO data){

        try{
            var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(),data.password());
            var auth = this.authenticationManager.authenticate(userNamePassword);
            var userDetailsImpl = (UserDetailsImpl)auth.getPrincipal();
            var jwtToken = this.tokenService.generateToken(userDetailsImpl.getUser());
            return ResponseEntity.ok(new LoginResponseDTO(jwtToken));
        }catch(AuthenticationException ex){
            // Log it, return error
            System.out.println("Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Invalid credentials", ErrorAssetEnum.AUTHENTICATION));
        }

    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterDTO data){

        if(this.userRepository.findByEmailOrUsername(data.email(), data.username()).isPresent()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage("There is already an user with that email.", ErrorAssetEnum.AUTHENTICATION));
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        User user = new User(data.username(),data.email(),encryptedPassword);

        this.userRepository.save(user);


        return ResponseEntity.ok().build();

    }

}
