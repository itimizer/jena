package com.itimizer.jena.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Defines the two thread pools for Jira work: {@code jiraPollExecutor} bounds parallel INCREMENTAL
 * filter polling ({@code jena.jira.poll.concurrency}) and {@code snapshotTaskScheduler} runs the
 * per-filter SNAPSHOT cron triggers ({@code jena.jira.snapshot.scheduler.pool-size}).
 */
@Configuration
@RequiredArgsConstructor
public class JiraPollConfig {

    private final ApplicationProperties applicationProperties;

    @Bean(destroyMethod = "shutdown")
    public ExecutorService jiraPollExecutor() {
        var concurrency = applicationProperties.getJira().getPoll().getConcurrency();
        return Executors.newFixedThreadPool(concurrency,
                new CustomizableThreadFactory("jira-poll-"));
    }

    @Bean
    public TaskScheduler snapshotTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(
                applicationProperties.getJira().getSnapshot().getScheduler().getPoolSize());
        scheduler.setThreadNamePrefix("snapshot-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }
}