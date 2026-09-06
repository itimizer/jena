package com.itimizer.jena.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationProperties validation")
class ApplicationPropertiesValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("should fail to start when jena.jira.url is blank")
    void should_fail_when_jira_url_is_blank() {
        contextRunner
                .withPropertyValues("jena.jira.url=")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("jena.jira.url"));
    }

    @Test
    @DisplayName("should fail to start when jena.jira config is entirely absent")
    void should_fail_when_jira_config_is_absent() {
        contextRunner
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("jena.jira"));
    }

    @Test
    @DisplayName("should start when jena.jira.url is present and other properties are absent")
    void should_start_when_only_jira_url_present() {
        contextRunner
                .withPropertyValues("jena.jira.url=https://jira.example.com")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("should default deployment-type to SERVER when absent")
    void should_default_deployment_type_to_server() {
        contextRunner
                .withPropertyValues("jena.jira.url=https://jira.example.com")
                .run(context -> assertThat(context)
                        .getBean(ApplicationProperties.class)
                        .extracting(properties -> properties.getJira().getDeploymentType())
                        .isEqualTo(ApplicationProperties.Jira.DeploymentType.SERVER));
    }

    @Test
    @DisplayName("should bind deployment-type cloud")
    void should_bind_deployment_type_cloud() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.jira.deployment-type=cloud")
                .run(context -> assertThat(context)
                        .getBean(ApplicationProperties.class)
                        .extracting(properties -> properties.getJira().getDeploymentType())
                        .isEqualTo(ApplicationProperties.Jira.DeploymentType.CLOUD));
    }

    @Test
    @DisplayName("should fail to start when deployment-type is not a known value")
    void should_fail_when_deployment_type_is_unknown() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.jira.deployment-type=datacenter")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("jena.jira.deployment-type"));
    }

    @Test
    @DisplayName("should fail to start when a PAT is configured against Jira Cloud")
    void should_fail_when_pat_configured_with_cloud() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://example.atlassian.net",
                        "jena.jira.deployment-type=cloud",
                        "jena.jira.pat=my-secret-token")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("Jira Cloud does not accept Personal Access "
                                + "Tokens"));
    }

    @Test
    @DisplayName("should start when Jira Cloud is configured with username and password")
    void should_start_when_cloud_configured_with_basic_auth() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://example.atlassian.net",
                        "jena.jira.deployment-type=cloud",
                        "jena.jira.username=user@example.com",
                        "jena.jira.password=my-api-token")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("should start when a PAT is configured against Jira Server")
    void should_start_when_pat_configured_with_server() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.jira.deployment-type=server",
                        "jena.jira.pat=my-secret-token")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("should fail to start when retryer is enabled but attempt is unset")
    void should_fail_when_retryer_enabled_without_attempt() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.retryer.enabled=true")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("when retryer is enabled"));
    }

    @Test
    @DisplayName("should fail to start when retryer.attempt is below 1")
    void should_fail_when_retryer_attempt_below_one() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.retryer.enabled=true",
                        "jena.retryer.attempt=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("should start when retryer is enabled with a positive attempt")
    void should_start_when_retryer_enabled_with_attempt() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.retryer.enabled=true",
                        "jena.retryer.attempt=3")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("should start when retryer is disabled and attempt is unset")
    void should_start_when_retryer_disabled_without_attempt() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.retryer.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("should fail to start when proxy.port is out of range")
    void should_fail_when_proxy_port_out_of_range() {
        contextRunner
                .withPropertyValues(
                        "jena.jira.url=https://jira.example.com",
                        "jena.proxy.port=70000")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasStackTraceContaining("port"));
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestConfig {
    }
}