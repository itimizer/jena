package com.itimizer.jena.controller;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.domain.TemplateField;
import com.itimizer.jena.dto.NotificationDto;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.itimizer.jena.entity.Channel.TELEGRAM;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = {ContainersConfig.class})
@WithMockUser(roles = "USER")
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Nested
    @DisplayName("resendNotification() method tests")
    class ResendNotificationTests {

        @Test
        @DisplayName("should resend notification")
        void should_resend_notification() throws Exception {
            ResponseEntity<String> successResponse =
                    ResponseEntity.ok("Notification resent successfully");
            when(notificationService.resendNotification(anyLong()))
                    .thenReturn(successResponse);

            mockMvc.perform(post("/notifications/1/resend"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Notification resent successfully"));
        }

        @Test
        @DisplayName("should return error when notification not found")
        void should_return_error_when_notification_not_found() throws Exception {
            when(notificationService.resendNotification(anyLong()))
                    .thenThrow(new ObjectNotFoundException(""));

            mockMvc.perform(post("/notifications/1/resend"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(post("/notifications/1/resend"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {})
        @DisplayName("should return forbidden when user no authorized")
        void should_return_forbidden_when_user_no_authorized() throws Exception {
            mockMvc.perform(post("/notifications/1/resend"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should propagate error status from service")
        void should_propagate_error_status_from_service() throws Exception {
            ResponseEntity<String> errorResponse = ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Failed to send notification");
            when(notificationService.resendNotification(456L)).thenReturn(errorResponse);

            mockMvc.perform(post("/notifications/456/resend")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Failed to send notification"));
        }

        @Test
        @DisplayName("should return error for unexpected service errors")
        void should_return_error_for_unexpected_service_errors() throws Exception {
            when(notificationService.resendNotification(123L))
                    .thenThrow(new RuntimeException("Unexpected error"));

            mockMvc.perform(post("/notifications/123/resend")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("dryRun() method tests")
    class DryRunTests {

        @Test
        @DisplayName("should dry-run notification")
        void should_dry_run_notification() throws Exception {
            NotificationDto notificationDto = new NotificationDto(
                    1L,
                    1L,
                    TELEGRAM,
                    "TST-1",
                    1L,
                    1,
                    "message"
            );

            List<NotificationDto> notificationList = List.of(notificationDto);
            when(notificationService.dryRun(1L, "TST-1", TELEGRAM))
                    .thenReturn(notificationList);

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].rule").value(1L))
                    .andExpect(jsonPath("$[0].template").value(1L))
                    .andExpect(jsonPath("$[0].channel").value("TELEGRAM"))
                    .andExpect(jsonPath("$[0].issueKey").value("TST-1"))
                    .andExpect(jsonPath("$[0].item").value(1L))
                    .andExpect(jsonPath("$[0].itemIndex").value(1))
                    .andExpect(jsonPath("$[0].message").value("message"));
        }

        @Test
        @DisplayName("should return empty list when no notifications generated")
        void should_return_empty_list_when_no_notifications_generated() throws Exception {
            when(notificationService.dryRun(1L, "TST-1", Channel.TELEGRAM))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
            verify(notificationService).dryRun(1L, "TST-1", Channel.TELEGRAM);
        }

        @Test
        @DisplayName("should return bad request when template has no content for channel")
        void should_return_bad_request_when_template_has_no_content_for_channel()
                throws Exception {
            when(notificationService.dryRun(1L, "TST-1", TELEGRAM))
                    .thenThrow(new ValidationException(
                            "Template 1 has no content for channel TELEGRAM."));

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.details",
                            containsString("has no content for channel TELEGRAM")));
        }

        @Test
        @DisplayName("should return not found when rule does not exist")
        void should_return_not_found_when_rule_does_not_exist() throws Exception {
            when(notificationService.dryRun(1L, "TST-1", TELEGRAM))
                    .thenThrow(new ObjectNotFoundException(
                            "Notification rule with id 1 not found."));

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.details",
                            containsString("Notification rule with id 1 not found")));
        }

        @Test
        @DisplayName("should handle TELEGRAM channel case insensitive")
        void should_handle_telegram_channel_case_insensitive() throws Exception {
            when(notificationService.dryRun(eq(1L), eq("TST-1"), eq(Channel.TELEGRAM)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "Telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(notificationService, times(3)).dryRun(1L, "TST-1", Channel.TELEGRAM);
        }

        @Test
        @DisplayName("should handle JIRAEMAIL channel")
        void should_handle_jiraEmail_channel() throws Exception {
            NotificationDto notification = new NotificationDto(
                    1L,
                    1L,
                    Channel.JIRAEMAIL,
                    "TST-1",
                    10000L,
                    0,
                    "Email content");
            when(notificationService.dryRun(1L, "TST-1", Channel.JIRAEMAIL))
                    .thenReturn(List.of(notification));

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "jiraEmail")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].channel", is("JIRAEMAIL")));
            verify(notificationService).dryRun(1L, "TST-1", Channel.JIRAEMAIL);
        }

        @Test
        @DisplayName("should return Bad Request for invalid channel")
        void should_return_bad_request_for_invalid_channel() throws Exception {
            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "invalid")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Invalid channel specified")))
                    .andExpect(content().string(containsString("Available channels")));
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("should handle missing required parameters")
        void should_handle_missing_required_parameters() throws Exception {
            mockMvc.perform(post("/notifications/dry-run")
                            .param("issueKey", "TST-1")
                            .param("channel", "telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("channel", "telegram")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(notificationService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {})
        @DisplayName("should return forbidden when user no authorized")
        void should_return_forbidden_when_user_no_authorized() throws Exception {
            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return error for unexpected service errors")
        void should_return_error_for_unexpected_service_errors() throws Exception {
            when(notificationService.dryRun(1L, "TST-1", TELEGRAM))
                    .thenThrow(new RuntimeException("Unexpected error"));

            mockMvc.perform(post("/notifications/dry-run")
                            .param("ruleId", "1")
                            .param("issueKey", "TST-1")
                            .param("channel", "TELEGRAM")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("getContext() method tests")
    class GetContextTests {

        @Test
        @DisplayName("should return latest context when historyId is omitted")
        void should_return_latest_context_when_history_id_is_omitted() throws Exception {
            when(notificationService.getContext(eq("TST-1"), isNull()))
                    .thenReturn(Map.of("key", new TemplateField("1000", "TST-1")));

            mockMvc.perform(get("/notifications/TST-1/context")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.key.stringValue").value("TST-1"));
            verify(notificationService).getContext("TST-1", null);
        }

        @Test
        @DisplayName("should return context for a specific historyId")
        void should_return_context_for_specific_history_id() throws Exception {
            when(notificationService.getContext("TST-1", 10000L))
                    .thenReturn(Map.of("key", new TemplateField("1000", "TST-1")));

            mockMvc.perform(get("/notifications/TST-1/context")
                            .param("historyId", "10000")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.key.stringValue").value("TST-1"));
            verify(notificationService).getContext("TST-1", 10000L);
        }

        @Test
        @DisplayName("should return not found when issue does not exist")
        void should_return_not_found_when_issue_does_not_exist() throws Exception {
            when(notificationService.getContext(any(), any()))
                    .thenThrow(new ObjectNotFoundException(
                            "Cannot get jira issue with key TST-1."));

            mockMvc.perform(get("/notifications/TST-1/context")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return bad request when historyId is not a number")
        void should_return_bad_request_when_history_id_is_not_numeric() throws Exception {
            mockMvc.perform(get("/notifications/TST-1/context")
                            .param("historyId", "abc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(notificationService);
        }

        @Test
        @WithAnonymousUser
        @DisplayName("should return error when unauthenticated")
        void should_return_error_when_unauthenticated() throws Exception {
            mockMvc.perform(get("/notifications/TST-1/context")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = {})
        @DisplayName("should return forbidden when user no authorized")
        void should_return_forbidden_when_user_no_authorized() throws Exception {
            mockMvc.perform(get("/notifications/TST-1/context")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}