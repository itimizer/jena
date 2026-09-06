package com.itimizer.jena.controller;

import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.dto.JiraFilterCreateDto;
import com.itimizer.jena.dto.JiraFilterDto;
import com.itimizer.jena.dto.NotificationTargetCreateDto;
import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.TemplateContentDto;
import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.FilterMode;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.repository.JiraFilterRepository;
import com.itimizer.jena.repository.NotificationTargetRepository;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import com.itimizer.jena.service.JiraFilterService;
import com.itimizer.jena.service.NotificationTargetService;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.service.TemplateService;
import com.itimizer.jena.service.JqlValidator;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
@DisplayName("JiraFilterController Integration Tests")
class JiraFilterControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JiraFilterService jiraFilterService;
    @Autowired
    private NotificationTargetService notificationTargetService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private TemplateService templateService;

    @MockitoBean
    private JqlValidator jqlValidator;

    @Autowired
    private JiraFilterRepository jiraFilterRepository;
    @Autowired
    private RuleRepository ruleRepository;
    @Autowired
    private NotificationTargetRepository notificationTargetRepository;
    @Autowired
    private TemplateRepository templateRepository;

    private Long filterId;

    @AfterEach
    void tearDown() {
        jiraFilterRepository.deleteAll();
        ruleRepository.deleteAll();
        notificationTargetRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("SkipSetup")) {
            return;
        }
        filterId = jiraFilterService.create(new JiraFilterCreateDto(
                "Bugs", "project = TST", FilterMode.INCREMENTAL, null, null, null, true)).id();
    }

    private Long createTarget() {
        return notificationTargetService.create(new NotificationTargetCreateDto(
                "Target", Channel.TELEGRAM, "-100" + System.nanoTime(), true)).id();
    }

    private Long createRule() {
        Long templateId = templateService.create(new TemplateCreateDto(
                "tpl-" + System.nanoTime(), new TemplateContentDto(null, null, null))).id();
        return ruleService.create(new RuleCreateDto(
                templateId, true, Event.ISSUE_CREATED, "status",
                null, null, null, null, false)).id();
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateTests {

        @Test
        @DisplayName("should create filter successfully")
        void should_create_filter_successfully() throws Exception {
            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "Suggestions", "project = SUG", FilterMode.INCREMENTAL, null, null, null, true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Suggestions"))
                    .andExpect(jsonPath("$.jql").value("project = SUG"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("should return 400 when JQL is invalid")
        void should_return_bad_request_when_jql_invalid() throws Exception {
            doThrow(new ValidationException("Invalid JQL"))
                    .when(jqlValidator).validate(eq("project = !!!"));

            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "Bad", "project = !!!", FilterMode.INCREMENTAL, null, null, null, true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void should_return_bad_request_when_name_blank() throws Exception {
            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "", "project = TST", FilterMode.INCREMENTAL, null, null, null, true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when name already exists")
        void should_return_conflict_on_duplicate_name() throws Exception {
            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "Bugs", "project = OTHER", FilterMode.INCREMENTAL, null, null, null, true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("update()/get()/list()/delete() method tests")
    class CrudTests {

        @Test
        @DisplayName("should update filter successfully")
        void should_update_filter_successfully() throws Exception {
            JiraFilterDto dto = new JiraFilterDto(
                    filterId, "Bugs Renamed", "project = TST2", FilterMode.INCREMENTAL,
                    null, null, null, false, null, null);

            mockMvc.perform(put("/filters/" + filterId)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Bugs Renamed"))
                    .andExpect(jsonPath("$.jql").value("project = TST2"))
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("should return 400 when path id does not match body id")
        void should_return_bad_request_on_id_mismatch() throws Exception {
            JiraFilterDto dto = new JiraFilterDto(
                    filterId, "Bugs", "project = TST", FilterMode.INCREMENTAL,
                    null, null, null, true, null, null);

            mockMvc.perform(put("/filters/" + (filterId + 1))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should get filter by id")
        void should_get_filter_by_id() throws Exception {
            mockMvc.perform(get("/filters/" + filterId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(filterId))
                    .andExpect(jsonPath("$.name").value("Bugs"));
        }

        @Test
        @DisplayName("should return 404 when filter not found")
        void should_return_not_found_when_missing() throws Exception {
            mockMvc.perform(get("/filters/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should delete filter and return 204")
        void should_delete_filter() throws Exception {
            mockMvc.perform(delete("/filters/" + filterId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/filters/" + filterId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Tag("SkipSetup")
        @WithAnonymousUser
        @DisplayName("should return 401 when unauthenticated")
        void should_return_unauthorized() throws Exception {
            mockMvc.perform(get("/filters"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("attach/detach target and rule tests")
    class LinkTests {

        @Test
        @DisplayName("should attach and detach a target")
        void should_attach_and_detach_target() throws Exception {
            Long targetId = createTarget();

            mockMvc.perform(put("/filters/" + filterId + "/targets/" + targetId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/filters/" + filterId))
                    .andExpect(jsonPath("$.targetIds[0]").value(targetId));

            mockMvc.perform(delete("/filters/" + filterId + "/targets/" + targetId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/filters/" + filterId))
                    .andExpect(jsonPath("$.targetIds").isEmpty());
        }

        @Test
        @DisplayName("should attach and detach a rule")
        void should_attach_and_detach_rule() throws Exception {
            Long ruleId = createRule();

            mockMvc.perform(put("/filters/" + filterId + "/rules/" + ruleId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/filters/" + filterId))
                    .andExpect(jsonPath("$.ruleIds[0]").value(ruleId));

            mockMvc.perform(delete("/filters/" + filterId + "/rules/" + ruleId))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/filters/" + filterId))
                    .andExpect(jsonPath("$.ruleIds").isEmpty());
        }

        @Test
        @DisplayName("should return 404 when attaching a missing target")
        void should_return_not_found_when_attaching_missing_target() throws Exception {
            mockMvc.perform(put("/filters/" + filterId + "/targets/" + Long.MAX_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when attaching a rule to a missing filter")
        void should_return_not_found_when_attaching_to_missing_filter() throws Exception {
            Long ruleId = createRule();

            mockMvc.perform(put("/filters/" + Long.MAX_VALUE + "/rules/" + ruleId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("snapshot filter tests")
    class SnapshotTests {

        private Long createTemplate() {
            return templateService.create(new TemplateCreateDto(
                    "snap-tpl-" + System.nanoTime(),
                    new TemplateContentDto(null, null, null))).id();
        }

        @Test
        @DisplayName("should create a SNAPSHOT filter with template and cron")
        void should_create_snapshot_filter() throws Exception {
            Long templateId = createTemplate();
            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "Daily Digest", "project = TST", FilterMode.SNAPSHOT,
                    templateId, "0 0 8 * * *", "UTC", true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mode").value("SNAPSHOT"))
                    .andExpect(jsonPath("$.templateId").value(templateId))
                    .andExpect(jsonPath("$.scheduleCron").value("0 0 8 * * *"));
        }

        @Test
        @DisplayName("should return 400 when SNAPSHOT filter has no template")
        void should_reject_snapshot_without_template() throws Exception {
            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "No Template", "project = TST", FilterMode.SNAPSHOT,
                    null, "0 0 8 * * *", "UTC", true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when INCREMENTAL filter has a cron")
        void should_reject_incremental_with_cron() throws Exception {
            JiraFilterCreateDto dto = new JiraFilterCreateDto(
                    "Bad Incremental", "project = TST", FilterMode.INCREMENTAL,
                    null, "0 0 8 * * *", null, true);

            mockMvc.perform(post("/filters")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when attaching a rule to a SNAPSHOT filter")
        void should_reject_rule_attach_to_snapshot() throws Exception {
            Long templateId = createTemplate();
            Long snapshotId = jiraFilterService.create(new JiraFilterCreateDto(
                    "Snap", "project = TST", FilterMode.SNAPSHOT,
                    templateId, "0 0 8 * * *", "UTC", true)).id();
            Long ruleId = createRule();

            mockMvc.perform(put("/filters/" + snapshotId + "/rules/" + ruleId))
                    .andExpect(status().isBadRequest());
        }
    }
}