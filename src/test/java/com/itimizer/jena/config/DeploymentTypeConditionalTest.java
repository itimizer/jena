package com.itimizer.jena.config;

import com.itimizer.jena.service.JiraSearchClient;
import com.itimizer.jena.service.JqlValidator;
import com.itimizer.jena.service.impl.JiraSearchClientV2;
import com.itimizer.jena.service.impl.JiraSearchClientV3;
import com.itimizer.jena.service.impl.JqlValidatorV2;
import com.itimizer.jena.service.impl.JqlValidatorV3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("jena.jira.deployment-type bean selection")
class DeploymentTypeConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean("jiraWebClient", WebClient.class, () -> WebClient.builder().build())
            .withUserConfiguration(SearchClientsConfig.class);

    @Test
    @DisplayName("should wire the v2 beans when deployment-type is absent")
    void should_wire_v2_beans_when_absent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JiraSearchClient.class);
            assertThat(context).hasSingleBean(JqlValidator.class);
            assertThat(context).getBean(JiraSearchClient.class)
                    .isInstanceOf(JiraSearchClientV2.class);
            assertThat(context).getBean(JqlValidator.class).isInstanceOf(JqlValidatorV2.class);
            assertThat(context).doesNotHaveBean(JiraSearchClientV3.class);
            assertThat(context).doesNotHaveBean(JqlValidatorV3.class);
        });
    }

    @Test
    @DisplayName("should wire the v2 beans when deployment-type is server")
    void should_wire_v2_beans_when_server() {
        contextRunner
                .withPropertyValues("jena.jira.deployment-type=server")
                .run(context -> {
                    assertThat(context).hasSingleBean(JiraSearchClient.class);
                    assertThat(context).hasSingleBean(JqlValidator.class);
                    assertThat(context).getBean(JiraSearchClient.class)
                            .isInstanceOf(JiraSearchClientV2.class);
                    assertThat(context).getBean(JqlValidator.class)
                            .isInstanceOf(JqlValidatorV2.class);
                    assertThat(context).doesNotHaveBean(JiraSearchClientV3.class);
                    assertThat(context).doesNotHaveBean(JqlValidatorV3.class);
                });
    }

    @Test
    @DisplayName("should wire the v3 beans when deployment-type is cloud")
    void should_wire_v3_beans_when_cloud() {
        contextRunner
                .withPropertyValues("jena.jira.deployment-type=cloud")
                .run(context -> {
                    assertThat(context).hasSingleBean(JiraSearchClient.class);
                    assertThat(context).hasSingleBean(JqlValidator.class);
                    assertThat(context).getBean(JiraSearchClient.class)
                            .isInstanceOf(JiraSearchClientV3.class);
                    assertThat(context).getBean(JqlValidator.class)
                            .isInstanceOf(JqlValidatorV3.class);
                    assertThat(context).doesNotHaveBean(JiraSearchClientV2.class);
                    assertThat(context).doesNotHaveBean(JqlValidatorV2.class);
                });
    }

    @Test
    @DisplayName("should wire nothing when deployment-type is not a known value")
    void should_wire_nothing_when_unknown() {
        contextRunner
                .withPropertyValues("jena.jira.deployment-type=datacenter")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JiraSearchClient.class);
                    assertThat(context).doesNotHaveBean(JqlValidator.class);
                });
    }

    @Configuration
    @Import({JiraSearchClientV2.class, JiraSearchClientV3.class,
             JqlValidatorV2.class, JqlValidatorV3.class})
    static class SearchClientsConfig {
    }
}