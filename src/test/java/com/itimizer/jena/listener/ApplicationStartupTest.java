package com.itimizer.jena.listener;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {ContainersConfig.class})
@TestPropertySource(properties = {
    "jena.security.create=true",
    "jena.security.username=integration-admin",
    "jena.security.password=integration-password"
})
@DisplayName("ApplicationStartup Integration Test")
class ApplicationStartupTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("should create admin user on application startup")
    void should_create_admin_user_on_application_startup() {
        await().untilAsserted(() -> {
            UserDetails user = userService.findUserByUsername("integration-admin");
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("integration-admin");
            assertThat(user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals))
                    .isTrue();
        });
    }
}