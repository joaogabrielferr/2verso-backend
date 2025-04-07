package com._verso._verso.auth.controller;

import com._verso._verso.auth.DTOs.LoginDTO;
import com._verso._verso.auth.DTOs.RegisterDTO;
import com._verso._verso.auth.jwt.TokenService;
import com._verso._verso.auth.model.User;
import com._verso._verso.auth.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginDTO data){
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(),data.password());
        var auth = this.authenticationManager.authenticate(userNamePassword);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data){

        if(this.userRepository.findByEmailOrUsername(data.email(), data.username()) != null){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        User user = new User(data.username(),data.email(),encryptedPassword);

        this.userRepository.save(user);

        String jwt = tokenService.generateToken(user);

        return ResponseEntity.ok(jwt);

    }

}
