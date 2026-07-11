package com.itimizer.jena.repository;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.entity.JiraFilter;
import com.itimizer.jena.entity.JiraSearchRun;
import com.itimizer.jena.entity.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("JiraSearchRun Repository Integration Tests")
class JiraSearchRunRepositoryTest {

    @Autowired
    private JiraSearchRunRepository repository;
    @Autowired
    private JiraFilterRepository jiraFilterRepository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
        jiraFilterRepository.deleteAll();
    }

    @AfterEach
    void teardown() {
        repository.deleteAll();
        jiraFilterRepository.deleteAll();
    }

    @Test
    @DisplayName("should return the watermark of the requested filter")
    void should_return_run_by_filter() {
        JiraFilter filterA = newFilter("Filter A");
        JiraFilter filterB = newFilter("Filter B");

        JiraSearchRun runA = newRun(filterA, "https://a.example", Status.SUCCESS);
        newRun(filterB, "https://b.example", Status.ERROR);

        JiraSearchRun result = repository.findByFilterId(filterA.getId());
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(runA.getId());
        assertThat(result.getBaseUrl()).isEqualTo("https://a.example");
        assertThat(result.getFilter().getId()).isEqualTo(filterA.getId());
    }

    private JiraFilter newFilter(String name) {
        JiraFilter f = new JiraFilter();
        f.setName(name);
        f.setJql("project = TST");
        f.setEnabled(true);
        return jiraFilterRepository.save(f);
    }

    private JiraSearchRun newRun(JiraFilter filter, String baseUrl, Status status) {
        JiraSearchRun run = new JiraSearchRun();
        run.setFilter(filter);
        run.setBaseUrl(baseUrl);
        run.setLastRunTime(LocalDateTime.now());
        run.setStatus(status);
        run.setFailureCount(0);
        return repository.save(run);
    }
}