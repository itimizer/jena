package com.itimizer.jena.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpringSecurityAuditorAware Tests")
class SpringSecurityAuditorAwareTest {

    private final SpringSecurityAuditorAware auditorAware = new SpringSecurityAuditorAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should return username when authenticated")
    void should_return_username_when_authenticated() {
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> result = auditorAware.getCurrentAuditor();
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("should return empty when not authenticated")
    void should_return_empty_when_not_authenticated() {
        SecurityContextHolder.clearContext();

        Optional<String> result = auditorAware.getCurrentAuditor();
        assertThat(result).isEmpty();
    }
}
