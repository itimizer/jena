package com.itimizer.jena.controller;

import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.UserCreateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("UserController Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should register user")
    void should_register_user() throws Exception {
        UserCreateDto requestDto = new UserCreateDto(
                "testuser",
                "password".toCharArray(),
                Collections.singleton("USER")
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/user/register")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().string("User testuser registered successfully"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("should return error on register user when json invalid")
    void should_return_error_on_register_user_when_json_invalid() throws Exception {
        String invalidJson = "";

        mockMvc.perform(post("/user/register")
                        .contentType(APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return error when unauthenticated")
    void should_return_error_when_unauthenticated() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("should return forbidden when user no authorized")
    void should_return_forbidden_when_user_no_authorized() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}