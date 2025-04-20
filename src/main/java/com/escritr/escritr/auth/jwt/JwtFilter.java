package com.escritr.escritr.auth.jwt;

import com.escritr.escritr.auth.model.User;
import com.escritr.escritr.auth.repository.UserRepository;
import com.escritr.escritr.auth.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.getToken(request);
        if(token != null){
            var login = tokenService.validateToken(token);
            Optional<User> user = userRepository.findByEmailOrUsername(login,login);
            if(user.isPresent()){
                System.out.println(user.get().getEmail());
                System.out.println(user.get().getUsername());
            }else{
                System.out.println("nao achou, login:" + login);
            }
            UserDetailsImpl userDetails = new UserDetailsImpl(user.orElseThrow());
            var authentication = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("Auth set in context: " + SecurityContextHolder.getContext().getAuthentication());
        }
        filterChain.doFilter(request,response);
    }

    private String getToken(HttpServletRequest request){
        var authHeader = request.getHeader("Authorization");
        if(authHeader == null)return null;
        return authHeader.replace("Bearer ","");
    }



}
