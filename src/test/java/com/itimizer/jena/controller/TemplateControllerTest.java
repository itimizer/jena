package com.itimizer.jena.controller;

import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.ExpressTemplateDto;
import com.itimizer.jena.dto.JiraEmailTemplateDto;
import com.itimizer.jena.dto.TelegramTemplateDto;
import com.itimizer.jena.dto.TemplateContentDto;
import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
@DisplayName("TemplateController Integration Tests")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private Long templateId;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        if (testInfo.getTags().contains("SkipSetup")) {
            return;
        }

        TemplateCreateDto requestDto = new TemplateCreateDto(
                "template",
                new TemplateContentDto(
                        new TelegramTemplateDto("telegram"),
                        new JiraEmailTemplateDto("email"),
                        new ExpressTemplateDto("express")
                )
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);

        MvcResult response = mockMvc.perform(post("/templates")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = response.getResponse().getContentAsString();
        TemplateDto templateDto = objectMapper.readValue(responseContent, TemplateDto.class);
        templateId = templateDto.id();
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateTests {

        @Test
        @DisplayName("should create template successfully")
        void should_create_template_successfully() throws Exception {
            TemplateCreateDto requestDto = new TemplateCreateDto(
                    "template",
                    new TemplateContentDto(
                            new TelegramTemplateDto("telegram"),
                            new JiraEmailTemplateDto("email"),
                            new ExpressTemplateDto("express")
                    )
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(post("/templates")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(equalTo(templateId))))
                    .andExpect(jsonPath("$.name").value("template"))
                    .andExpect(jsonPath("$.content.telegram.message").value("telegram"))
                    .andExpect(jsonPath("$.content.jira-email.message").value("email"));
        }

        @Test
        @DisplayName("should return error when mandatory field is null")
        void should_return_error_when_mandatory_field_is_null() throws Exception {
            String requestJson =
                    """
                        {
                          "name": null,
                          "content": {
                            "telegram": {
                              "message": "telegram"
                            },
                            "jira-email": {
                              "message": "email"
                            }
                          }
                        }
                    """;

            mockMvc.perform(post("/templates")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when mandatory field is missing")
        void should_return_error_when_mandatory_field_is_missing() throws Exception {
            String requestJson =
                    """
                        {
                          "name": "test template"
                        }
                    """;

            mockMvc.perform(post("/templates")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when json invalid")
        void should_return_error_when_json_invalid() throws Exception {
            String invalidJson = "";

            mockMvc.perform(post("/templates")
                            .contentType(APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when value exceeds max length")
        void should_return_error_when_value_exceeds_max_length() throws Exception {
            String veryLongName = "a".repeat(300);
            String requestJson =
                """
                    {
                      "name": "%s",
                      "content": {
                        "telegram": {
                          "message": "test"
                        },
                        "jira-email": {
                          "message": "test"
                        }
                      }
                    }
                """.formatted(veryLongName);

            mockMvc.perform(post("/templates")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("update() method tests")
    class UpdateTests {

        @Test
        @DisplayName("should update template successfully")
        void should_update_template_successfully() throws Exception {
            TemplateDto requestDto = new TemplateDto(
                    templateId,
                    "updated-template",
                    new TemplateContentDto(
                            new TelegramTemplateDto("updated-telegram"),
                            new JiraEmailTemplateDto("updated-email"),
                            new ExpressTemplateDto("updated-express")
                    )
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/templates/" + templateId)
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(templateId))
                    .andExpect(jsonPath("$.name").value("updated-template"))
                    .andExpect(jsonPath("$.content.telegram.message").value("updated-telegram"))
                    .andExpect(jsonPath("$.content.jira-email.message").value("updated-email"));
        }

        @Test
        @DisplayName("should return error when template not found")
        void should_return_error_when_template_not_found() throws Exception {
            TemplateDto requestDto = new TemplateDto(
                    Long.MAX_VALUE,
                    "updated-template",
                    new TemplateContentDto(
                            new TelegramTemplateDto("updated-telegram"),
                            new JiraEmailTemplateDto("updated-email"),
                            new ExpressTemplateDto("updated-express")
                    )
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/templates/" + Long.MAX_VALUE)
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return error when mandatory field is null")
        void should_return_error_when_mandatory_field_is_null() throws Exception {
            String requestJson =
                    """
                        {
                          "id": null,
                          "name": "test template",
                          "content": {
                            "telegram": {
                              "message": "telegram"
                            },
                            "jira-email": {
                              "message": "email"
                            }
                          }
                        }
                    """;

            mockMvc.perform(put("/templates/1")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when mandatory field is missing")
        void should_return_error_when_mandatory_field_is_missing() throws Exception {
            String requestJson =
                    """
                        {
                          "name": "test template",
                          "content": {
                            "telegram": {
                              "message": "telegram"
                            },
                            "jira-email": {
                              "message": "email"
                            }
                          }
                        }
                    """;

            mockMvc.perform(put("/templates/1")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when json invalid")
        void should_return_error_when_json_invalid() throws Exception {
            String invalidJson = "";

            mockMvc.perform(put("/templates/1")
                            .contentType(APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when value exceeds max length")
        void should_return_error_when_value_exceeds_max_length() throws Exception {
            String veryLongName = "a".repeat(300);
            String requestJson =
                    """
                        {
                          "id": %d,
                          "name": "%s",
                          "content": {
                            "telegram": {
                              "message": "test"
                            },
                            "jira-email": {
                              "message": "test"
                            }
                          }
                        }
                    """.formatted(templateId, veryLongName);

            mockMvc.perform(put("/templates/" + templateId)
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("get() method tests")
    class GetTests {

        @Test
        @DisplayName("should get template by id")
        void should_get_template_by_id() throws Exception {
            mockMvc.perform(get("/templates/" + templateId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(templateId))
                    .andExpect(jsonPath("$.name").value("template"))
                    .andExpect(jsonPath("$.content.telegram.message").value("telegram"))
                    .andExpect(jsonPath("$.content.jira-email.message").value("email"));
        }

        @Test
        @DisplayName("should return error when template not found")
        void should_return_error_when_template_not_found() throws Exception {
            mockMvc.perform(get("/templates/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/templates/" + Long.MAX_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Tag("SkipSetup")
        @WithMockUser(roles = {})
        @DisplayName("should return forbidden when user no authorized")
        void should_return_forbidden_when_user_no_authorized() throws Exception {
            mockMvc.perform(get("/templates/" + Long.MAX_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("list() method tests")
    class ListTests {

        @Test
        @DisplayName("should list templates including the created template")
        void should_list_templates() throws Exception {
            mockMvc.perform(get("/templates"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == %d)]".formatted(templateId), hasSize(1)));
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/templates"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("delete() method tests")
    class DeleteTests {

        @Test
        @DisplayName("should delete template and return 204")
        void should_delete_template_successfully() throws Exception {
            mockMvc.perform(delete("/templates/" + templateId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/templates/" + templateId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return not found when deleting non-existent template")
        void should_return_not_found_when_deleting_missing_template() throws Exception {
            mockMvc.perform(delete("/templates/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when deleting a template referenced by a rule")
        void should_return_conflict_when_template_referenced_by_rule() throws Exception {
            String ruleJson =
                    """
                    {
                      "template": %d,
                      "enabled": true,
                      "event": "ISSUE_CREATED"
                    }
                    """.formatted(templateId);
            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(ruleJson))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete("/templates/" + templateId))
                    .andExpect(status().isConflict());
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(delete("/templates/" + Long.MAX_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }
}