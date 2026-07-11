package com.itimizer.jena.listener;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Bootstraps the admin account on startup. When {@code jena.security.create} is enabled and
 * credentials are configured, registers the admin user (with role {@code ADMIN}) unless it already
 * exists. Idempotent across restarts.
 */
@Slf4j
@Component
@AllArgsConstructor
public class ApplicationStartup {

    private final ApplicationProperties applicationProperties;
    private final UserService userService;

    @EventListener(ApplicationReadyEvent.class)
    public void initAdminUser() {
        log.info("initAdminUser invoked on application startup");
        if (applicationProperties.getSecurity() != null
                && applicationProperties.getSecurity().getCreate()
                && applicationProperties.getSecurity().getUsername() != null
                && applicationProperties.getSecurity().getPassword() != null) {
            var username = applicationProperties.getSecurity().getUsername();

            log.info("Admin user creation enabled. Checking for user [{}]", username);
            try {
                userService.findUserByUsername(username);
                log.info("Admin user [{}] already exists. No action needed.", username);
            } catch (UsernameNotFoundException e) {
                userService.registerUser(username,
                        applicationProperties.getSecurity().getPassword(),
                        Collections.singleton("ADMIN"));
                log.info("User [{}] is created with role ADMIN!", username);
            } catch (Exception ex) {
                log.error("Unexpected error during admin user initialization: {}",
                        ex.getMessage(), ex);
            }
        } else {
            log.info("Admin user creation skipped due to missing or invalid configuration.");
        }
    }

}
