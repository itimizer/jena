package com.itimizer.jena.health;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Actuator health contributor that probes Jira connectivity via {@code /serverInfo}: {@code UP}
 * when reachable, {@code DOWN} (with the exception class) on failure, {@code UNKNOWN} when no Jira
 * client is configured. Not part of the liveness/readiness groups.
 */
@Component
public class JiraHealthIndicator implements HealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final ObjectProvider<WebClient> jiraWebClientProvider;

    public JiraHealthIndicator(
            @Qualifier("jiraWebClient") ObjectProvider<WebClient> jiraWebClientProvider) {
        this.jiraWebClientProvider = jiraWebClientProvider;
    }

    @Override
    public Health health() {
        var jiraWebClient = jiraWebClientProvider.getIfAvailable();
        if (jiraWebClient == null) {
            return Health.unknown().withDetail("reason", "Jira web client not configured").build();
        }
        try {
            jiraWebClient.get()
                    .uri("/rest/api/2/serverInfo")
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .block();
            return Health.up().build();
        } catch (RuntimeException e) {
            return Health.down().withDetail("error", e.getClass().getSimpleName()).build();
        }
    }
}
