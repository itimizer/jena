package com.itimizer.jena.controller;

import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.dto.TemplateContentDto;
import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.service.TemplateService;
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

import static com.itimizer.jena.entity.Event.ISSUE_CREATED;
import static com.itimizer.jena.entity.Event.ISSUE_UPDATED;
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
@DisplayName("RuleController Integration Tests")
class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TemplateService templateService;

    private Long templateId;
    private Long ruleId;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        if (testInfo.getTags().contains("SkipSetup")) {
            return;
        }

        TemplateCreateDto templateDto = new TemplateCreateDto(
                "template",
                new TemplateContentDto(null, null, null)
        );
        templateId = templateService.create(templateDto).id();

        RuleCreateDto requestDto = new RuleCreateDto(
                templateId,
                true,
                ISSUE_CREATED,
                "status",
                "1",
                "Open",
                "2",
                "Resolved",
                false

        );
        String requestJson = objectMapper.writeValueAsString(requestDto);

        MvcResult response = mockMvc.perform(post("/rules")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = response.getResponse().getContentAsString();
        RuleDto ruleDto = objectMapper.readValue(responseContent, RuleDto.class);
        ruleId = ruleDto.id();
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateTests {

        @Test
        @DisplayName("should create rule successfully")
        void should_create_rule_successfully() throws Exception {
            RuleCreateDto requestDto = new RuleCreateDto(
                    templateId,
                    true,
                    ISSUE_CREATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false

            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", not(equalTo(ruleId))))
                    .andExpect(jsonPath("$.template").value(templateId))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.event").value("ISSUE_CREATED"))
                    .andExpect(jsonPath("$.field").value("status"))
                    .andExpect(jsonPath("$.from").value("1"))
                    .andExpect(jsonPath("$.fromString").value("Open"))
                    .andExpect(jsonPath("$.to").value("2"))
                    .andExpect(jsonPath("$.toString").value("Resolved"))
                    .andExpect(jsonPath("$.hasChanged").value(false));
        }

        @Test
        @DisplayName("should create rule when field is null for ISSUE_CREATED")
        void should_create_rule_when_field_is_null_for_issue_created() throws Exception {
            RuleCreateDto requestDto = new RuleCreateDto(
                    templateId,
                    true,
                    ISSUE_CREATED,
                    null,
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return error when field is null for ISSUE_UPDATED")
        void should_return_error_when_field_is_null_for_issue_updated() throws Exception {
            RuleCreateDto requestDto = new RuleCreateDto(
                    templateId,
                    true,
                    Event.ISSUE_UPDATED,
                    null,
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error on create when template not found")
        void should_return_error_on_create_when_template_not_found() throws Exception {
            RuleCreateDto requestDto = new RuleCreateDto(
                    Long.MAX_VALUE,
                    true,
                    ISSUE_CREATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false

            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return error when json invalid")
        void should_return_error_on_create_rule_when_json_invalid() throws Exception {
            String invalidJson = "";

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when mandatory field is missing")
        void should_return_error_when_mandatory_field_is_missing() throws Exception {
            String requestJson =
                """
                    {
                      "enabled": true,
                      "event": "ISSUE_UPDATED",
                      "field": "assignee",
                      "from": null,
                      "fromString": ".*",
                      "to": null,
                      "toString": ".*",
                      "hasChanged": true
                    }
                """;

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when mandatory field is null")
        void should_return_error_when_mandatory_field_is_null() throws Exception {
            String requestJson =
                """
                    {
                      "template": 1,
                      "enabled": true,
                      "event": null,
                      "field": "assignee",
                      "from": null,
                      "fromString": ".*",
                      "to": null,
                      "toString": ".*",
                      "hasChanged": true
                    }
                """;

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when value exceeds max length")
        void should_return_error_when_value_exceeds_max_length() throws Exception {
            String veryLongField = "a".repeat(300);
            String requestJson =
                    """
                        {
                          "template": 1,
                          "enabled": true,
                          "event": "ISSUE_UPDATED",
                          "field": "%s",
                          "from": null,
                          "fromString": ".*",
                          "to": null,
                          "toString": ".*",
                          "hasChanged": true
                        }
                    """.formatted(veryLongField);

            mockMvc.perform(post("/rules")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("update() method tests")
    class UpdateTests {

        @Test
        @DisplayName("should update rule successfully")
        void should_update_rule_successfully() throws Exception {
            RuleDto requestDto = new RuleDto(
                    ruleId,
                    templateId,
                    false,
                    ISSUE_UPDATED,
                    "updated-status",
                    "updated-1",
                    "updated-Open",
                    "updated-2",
                    "updated-Resolved",
                    true
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/rules/" + ruleId)
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ruleId))
                    .andExpect(jsonPath("$.template").value(templateId))
                    .andExpect(jsonPath("$.enabled").value(false))
                    .andExpect(jsonPath("$.event").value("ISSUE_UPDATED"))
                    .andExpect(jsonPath("$.field").value("updated-status"))
                    .andExpect(jsonPath("$.from").value("updated-1"))
                    .andExpect(jsonPath("$.fromString").value("updated-Open"))
                    .andExpect(jsonPath("$.to").value("updated-2"))
                    .andExpect(jsonPath("$.toString").value("updated-Resolved"))
                    .andExpect(jsonPath("$.hasChanged").value(true));
        }

        @Test
        @DisplayName("should return error when rule not found")
        void should_return_error_when_rule_not_found() throws Exception {
            RuleDto requestDto = new RuleDto(
                    Long.MAX_VALUE,
                    templateId,
                    true,
                    ISSUE_CREATED,
                    "status",
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/rules/" + Long.MAX_VALUE)
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return error when rule is missing")
        void should_return_error_when_rule_is_missing() throws Exception {
            String requestJson =
                """
                    {
                      "template": 1,
                      "enabled": true,
                      "event": "ISSUE_UPDATED",
                      "field": "assignee",
                      "from": null,
                      "fromString": ".*",
                      "to": null,
                      "toString": ".*",
                      "hasChanged": true
                    }
                """;

            mockMvc.perform(put("/rules/1")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should update rule when field is null for ISSUE_CREATED")
        void should_update_rule_when_field_is_null_for_issue_created() throws Exception {
            RuleDto requestDto = new RuleDto(
                    ruleId,
                    templateId,
                    true,
                    ISSUE_CREATED,
                    null,
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/rules/" + ruleId)
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return error when field is null for ISSUE_UPDATED")
        void should_return_error_when_field_is_null_for_issue_updated() throws Exception {
            RuleDto requestDto = new RuleDto(
                    ruleId,
                    templateId,
                    true,
                    Event.ISSUE_UPDATED,
                    null,
                    "1",
                    "Open",
                    "2",
                    "Resolved",
                    false
            );
            String requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/rules/" + ruleId)
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
                       "id": 1,
                      "template": 1,
                      "enabled": true,
                      "field": "assignee",
                      "from": null,
                      "fromString": ".*",
                      "to": null,
                      "toString": ".*",
                      "hasChanged": true
                    }
                """;

            mockMvc.perform(put("/rules/1")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when mandatory field is null")
        void should_return_error_when_mandatory_field_is_null() throws Exception {
            String requestJson =
                """
                    {
                      "id": 1,
                      "template": 1,
                      "enabled": true,
                      "event": null,
                      "field": "assignee",
                      "from": null,
                      "fromString": ".*",
                      "to": null,
                      "toString": ".*",
                      "hasChanged": true
                    }
                """;

            mockMvc.perform(put("/rules/1")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return error when json invalid")
        void should_return_error_when_json_invalid() throws Exception {
            String invalidJson = "";

            mockMvc.perform(put("/rules/1")
                            .contentType(APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when value exceeds max length")
        void should_return_error_when_value_exceeds_max_length() throws Exception {
            String veryLongField = "a".repeat(300);
            String requestJson =
                    """
                        {
                          "id": 1,
                          "template": 1,
                          "enabled": true,
                          "event": "ISSUE_UPDATED",
                          "field": "%s",
                          "from": null,
                          "fromString": ".*",
                          "to": null,
                          "toString": ".*",
                          "hasChanged": true
                        }
                    """.formatted(veryLongField);

            mockMvc.perform(put("/rules/1")
                            .contentType(APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("get() method tests")
    class GetTests {

        @Test
        @DisplayName("should get rule by id successfully")
        void should_get_rule_by_id_successfully() throws Exception {
            mockMvc.perform(get("/rules/" + ruleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", not(equalTo(ruleId))))
                    .andExpect(jsonPath("$.template").value(templateId))
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.event").value("ISSUE_CREATED"))
                    .andExpect(jsonPath("$.field").value("status"))
                    .andExpect(jsonPath("$.from").value("1"))
                    .andExpect(jsonPath("$.fromString").value("Open"))
                    .andExpect(jsonPath("$.to").value("2"))
                    .andExpect(jsonPath("$.toString").value("Resolved"))
                    .andExpect(jsonPath("$.hasChanged").value(false));
        }

        @Test
        @DisplayName("should return error when rule not found")
        void should_return_error_when_template_not_found() throws Exception {
            mockMvc.perform(get("/rules/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/rules/" + Long.MAX_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Tag("SkipSetup")
        @WithMockUser(roles = {})
        @DisplayName("should return forbidden when user no authorized")
        void should_return_forbidden_when_user_no_authorized() throws Exception {
            mockMvc.perform(get("/rules/" + Long.MAX_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("list() method tests")
    class ListTests {

        @Test
        @DisplayName("should list rules including the created rule")
        void should_list_rules() throws Exception {
            mockMvc.perform(get("/rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.id == %d)]".formatted(ruleId), hasSize(1)));
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/rules"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("delete() method tests")
    class DeleteTests {

        @Test
        @DisplayName("should delete rule and return 204")
        void should_delete_rule_successfully() throws Exception {
            mockMvc.perform(delete("/rules/" + ruleId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/rules/" + ruleId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return not found when deleting non-existent rule")
        void should_return_not_found_when_deleting_missing_rule() throws Exception {
            mockMvc.perform(delete("/rules/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(delete("/rules/" + Long.MAX_VALUE))
                    .andExpect(status().isUnauthorized());
        }
    }
}