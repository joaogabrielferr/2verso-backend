package com.escritr.escritr.auth.service;

import com.escritr.escritr.auth.repository.UserRepository;
import com.escritr.escritr.auth.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        return userRepo
                .findByEmailOrUsername(input, input)
                .map(UserDetailsImpl::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email or username: " + input));
    }

}
