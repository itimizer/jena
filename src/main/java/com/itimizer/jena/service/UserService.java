package com.itimizer.jena.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Manages application accounts in the Spring Security JDBC user store (HTTP Basic auth). Usernames
 * are normalised to lower case on registration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public void registerUser(String username, String password, Collection<String> roles) {
        ((JdbcUserDetailsManager) userDetailsService)
                .createUser(User.builder()
                        .username(username.toLowerCase())
                        .password(passwordEncoder.encode(password))
                        .roles(roles.toArray(String[]::new))
                        .build());
    }

    public UserDetails findUserByUsername(String username) {
        return userDetailsService.loadUserByUsername(username);
    }
}
