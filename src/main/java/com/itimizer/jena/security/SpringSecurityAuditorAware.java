package com.itimizer.jena.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Supplies the current username to JPA auditing ({@code @CreatedBy}/{@code @LastModifiedBy}) from
 * the Spring Security context, so entity audit columns record who made each change.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public @NonNull Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .map(User.class::cast)
                .map(User::getUsername);
    }
}
