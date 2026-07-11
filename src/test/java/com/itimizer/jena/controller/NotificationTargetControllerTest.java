package com.itimizer.jena.controller;

import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.NotificationTargetCreateDto;
import com.itimizer.jena.dto.NotificationTargetDto;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.repository.NotificationTargetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = {ContainersConfig.class})
@WithMockUser(roles = "USER")
@DisplayName("NotificationTargetController Integration Tests")
class NotificationTargetControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NotificationTargetRepository notificationTargetRepository;

    private Long targetId;

    @AfterEach
    void tearDown() {
        notificationTargetRepository.deleteAll();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        if (testInfo.getTags().contains("SkipSetup")) {
            return;
        }
        NotificationTargetCreateDto dto = new NotificationTargetCreateDto(
                "QA Telegram", Channel.TELEGRAM, "-1001234567890", true);

        MvcResult response = mockMvc.perform(post("/targets")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        targetId = objectMapper.readValue(response.getResponse().getContentAsString(),
                NotificationTargetDto.class).id();
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateTests {

        @Test
        @DisplayName("should create target successfully")
        void should_create_target_successfully() throws Exception {
            NotificationTargetCreateDto dto = new NotificationTargetCreateDto(
                    "Dev Express", Channel.EXPRESS, "group-42", true);

            mockMvc.perform(post("/targets")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Dev Express"))
                    .andExpect(jsonPath("$.channel").value("EXPRESS"))
                    .andExpect(jsonPath("$.chatId").value("group-42"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void should_return_bad_request_when_name_blank() throws Exception {
            NotificationTargetCreateDto dto = new NotificationTargetCreateDto(
                    "", Channel.TELEGRAM, "12345", true);

            mockMvc.perform(post("/targets")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when channel and chatId already exist")
        void should_return_conflict_on_duplicate() throws Exception {
            NotificationTargetCreateDto dto = new NotificationTargetCreateDto(
                    "Duplicate", Channel.TELEGRAM, "-1001234567890", true);

            mockMvc.perform(post("/targets")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("update() method tests")
    class UpdateTests {

        @Test
        @DisplayName("should update target successfully")
        void should_update_target_successfully() throws Exception {
            NotificationTargetDto dto = new NotificationTargetDto(
                    targetId, "QA Telegram Renamed", Channel.TELEGRAM, "-1009999999999", false);

            mockMvc.perform(put("/targets/" + targetId)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(targetId))
                    .andExpect(jsonPath("$.name").value("QA Telegram Renamed"))
                    .andExpect(jsonPath("$.chatId").value("-1009999999999"))
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("should return 400 when path id does not match body id")
        void should_return_bad_request_on_id_mismatch() throws Exception {
            NotificationTargetDto dto = new NotificationTargetDto(
                    targetId, "QA Telegram", Channel.TELEGRAM, "-1001234567890", true);

            mockMvc.perform(put("/targets/" + (targetId + 1))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when target not found")
        void should_return_not_found_when_missing() throws Exception {
            NotificationTargetDto dto = new NotificationTargetDto(
                    Long.MAX_VALUE, "Missing", Channel.TELEGRAM, "1", true);

            mockMvc.perform(put("/targets/" + Long.MAX_VALUE)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("get()/list() method tests")
    class ReadTests {

        @Test
        @DisplayName("should get target by id")
        void should_get_target_by_id() throws Exception {
            mockMvc.perform(get("/targets/" + targetId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(targetId))
                    .andExpect(jsonPath("$.channel").value("TELEGRAM"));
        }

        @Test
        @DisplayName("should return 404 when target not found")
        void should_return_not_found_when_missing() throws Exception {
            mockMvc.perform(get("/targets/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should list targets including the created one")
        void should_list_targets() throws Exception {
            mockMvc.perform(get("/targets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == %d)]".formatted(targetId), hasSize(1)));
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return 401 when unauthenticated")
        void should_return_unauthorized() throws Exception {
            mockMvc.perform(get("/targets"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("delete() method tests")
    class DeleteTests {

        @Test
        @DisplayName("should delete target and return 204")
        void should_delete_target() throws Exception {
            mockMvc.perform(delete("/targets/" + targetId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/targets/" + targetId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent target")
        void should_return_not_found_when_deleting_missing() throws Exception {
            mockMvc.perform(delete("/targets/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }
    }
}