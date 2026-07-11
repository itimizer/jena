package com.itimizer.jena.config;

import com.itimizer.jena.thymeleaf.TemplateDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Configures the Thymeleaf {@code TemplateEngine} used to render notifications: an in-memory string
 * resolver (templates come from the DB, not files) plus JENA's custom {@link TemplateDialect} for
 * Jira-field access.
 */
@Configuration
@RequiredArgsConstructor
public class ThymeleafConfig {

    private final TemplateDialect templateDialect;

    @Bean
    public StringTemplateResolver templateResolver() {
        var templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        var templateEngine = new SpringTemplateEngine();
        templateEngine.addDialect(templateDialect);
        templateEngine.setTemplateResolver(templateResolver());
        return templateEngine;
    }
}
