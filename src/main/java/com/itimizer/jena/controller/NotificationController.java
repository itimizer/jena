package com.itimizer.jena.controller;

import com.itimizer.jena.dto.NotificationDto;
import com.itimizer.jena.entity.Channel;
import com.itimizer.jena.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API under {@code /notifications}: resend a failed notification, dry-run a rule's rendered
 * message and preview the field context for an issue. Thin adapter over
 * {@link NotificationService}.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/{id}/resend")
    public ResponseEntity<String> resendNotification(@PathVariable Long id) {
        return notificationService.resendNotification(id);
    }

    @PostMapping("/dry-run")
    public List<NotificationDto> dryRun(@RequestParam Long ruleId,
                                        @RequestParam String issueKey,
                                        @RequestParam Channel channel) {
        return notificationService.dryRun(ruleId, issueKey, channel);
    }

    @GetMapping("/{issueKey}/context")
    public Map<String, Object> getContext(@PathVariable String issueKey,
                                          @RequestParam(required = false) Long historyId) {
        return notificationService.getContext(issueKey, historyId);
    }

}
