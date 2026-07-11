package com.itimizer.jena.service;

import com.itimizer.jena.config.ContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = {ContainersConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserService Integration Tests")
class UserServiceTest {

    @Autowired
    UserService userService;

    @Test
    @Order(0)
    @DisplayName("should register user")
    void should_register_user() {
        assertThatNoException()
                .isThrownBy(() -> userService
                        .registerUser("testuser", "password", java.util.List.of("USER")));
    }

    @Test
    @DisplayName("should find user by username")
    void should_find_user_by_username() {
        assertThatNoException()
                .isThrownBy(() -> userService.findUserByUsername("testuser"));
    }

    @Test
    @DisplayName("should throw exception when user not found")
    void should_throw_exception_when_user_not_found() {
        assertThatThrownBy(() -> userService.findUserByUsername("notfounduser"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}