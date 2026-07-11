package com.itimizer.jena.controller;

import com.itimizer.jena.dto.UserCreateDto;
import com.itimizer.jena.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API under {@code /user} for registering application accounts. ADMIN-only (enforced by the
 * security config). Thin adapter over {@link UserService}.
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("register")
    public ResponseEntity<String> register(@RequestBody @Valid UserCreateDto user) {
        userService.registerUser(user.username(), String.valueOf(user.password()), user.roles());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User %s registered successfully".formatted(user.username()));
    }

}