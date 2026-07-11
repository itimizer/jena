package com.itimizer.jena.service.impl;

import com.itimizer.jena.config.ContainersConfig;
import com.itimizer.jena.domain.TemplateField;
import com.itimizer.jena.dto.ExpressTemplateDto;
import com.itimizer.jena.dto.JiraEmailTemplateDto;
import com.itimizer.jena.dto.TelegramTemplateDto;
import com.itimizer.jena.dto.TemplateContentDto;
import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
import com.itimizer.jena.entity.Template;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.repository.RuleRepository;
import com.itimizer.jena.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.thymeleaf.exceptions.TemplateInputException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser(roles = "USER")
@SpringBootTest(classes = {ContainersConfig.class})
@DisplayName("TemplateService Integration Tests")
class TemplateServiceImplTest {

    @Autowired
    private TemplateServiceImpl templateService;
    @Autowired
    private TemplateRepository templateRepository;
    @Autowired
    private RuleRepository ruleRepository;

    private TemplateCreateDto templateCreateDto;

    @BeforeEach
    void setUp() {
        templateCreateDto = new TemplateCreateDto(
                "template",
                new TemplateContentDto(
                        new TelegramTemplateDto("telegram"),
                        new JiraEmailTemplateDto("email"),
                        new ExpressTemplateDto("express")
                )
        );
    }

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
        ruleRepository.deleteAll();
    }

    @Nested
    @DisplayName("create() method tests")
    class CreateMethodTests {

        @Test
        @DisplayName("should create template successfully")
        void should_create_template_successfully() {
            TemplateDto result = templateService.create(templateCreateDto);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when DTO is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_dto_is_null() {
            assertThatThrownBy(() -> templateService.create(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw exception when DTO has empty name")
        void should_throw_exception_when_dto_has_empty_name() {
            TemplateCreateDto invalidDto = new TemplateCreateDto(
                    null,
                    new TemplateContentDto(
                            new TelegramTemplateDto("telegram"),
                            new JiraEmailTemplateDto("email"),
                            new ExpressTemplateDto("express")
                    )
            );

            assertThatThrownBy(() -> templateService.create(invalidDto))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("update() method tests")
    class UpdateMethodTests {

        @Test
        @DisplayName("should update template successfully")
        void should_update_template_successfully() {
            TemplateDto templateDto = templateService.create(templateCreateDto);

            TemplateDto updatedDto = new TemplateDto(
                    templateDto.id(),
                    "updated template",
                    new TemplateContentDto(
                            new TelegramTemplateDto("telegram"),
                            new JiraEmailTemplateDto("email"),
                            new ExpressTemplateDto("express")
                    )
            );

            TemplateDto result = templateService.update(updatedDto);
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(templateDto.id());
            assertThat(result.name()).isEqualTo("updated template");
        }

        @Test
        @DisplayName("should throw exception when updating non-existent template")
        void should_throw_exception_when_updating_non_existent_template() {
            TemplateDto nonExistentDto = new TemplateDto(
                    999L,
                    "non-existent template",
                    new TemplateContentDto(
                            new TelegramTemplateDto("telegram"),
                            new JiraEmailTemplateDto("email"),
                            new ExpressTemplateDto("express")
                    )
            );

            assertThatThrownBy(() -> templateService.update(nonExistentDto))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when update DTO is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_update_dto_is_null() {
            assertThatThrownBy(() -> templateService.update(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("get() method tests")
    class GetMethodTests {

        @Test
        @DisplayName("should get template by ID successfully")
        void should_get_template_by_id_successfully() {
            TemplateDto templateDto = templateService.create(templateCreateDto);

            TemplateDto result = templateService.get(templateDto.id());
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(templateDto.id());
            assertThat(result.name()).isEqualTo("template");
        }

        @Test
        @DisplayName("should throw ObjectNotFoundException when template not found")
        void should_throw_object_not_found_exception_when_template_not_found() {
            assertThatThrownBy(() -> templateService.get(999L))
                    .isInstanceOf(ObjectNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when ID is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_id_is_null() {
            assertThatThrownBy(() -> templateService.get(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("fetch() method tests")
    class FetchMethodTests {

        @Test
        @DisplayName("should fetch template successfully")
        void should_fetch_template_successfully() {
            TemplateDto templateDto = templateService.create(templateCreateDto);

            Template result = templateService.fetch(templateDto.id());
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(templateDto.id());
            assertThat(result.getName()).isEqualTo("template");
        }

        @Test
        @DisplayName("should return null when template not found")
        void should_return_null_when_template_not_found() {
            Template result = templateService.fetch(999L);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw exception when ID is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_id_is_null() {
            assertThatThrownBy(() -> templateService.fetch(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("getContent() method tests")
    class GetContentMethodTests {

        @Test
        @DisplayName("should process template with attributes successfully")
        void should_process_template_with_attributes_successfully() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("key",
                    new TemplateField("1000", "TST-1")
            );

            String result = templateService.getContent("[[${key?.value}]], [[${key?.stringValue}]]",
                    attributes);
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo("1000, TST-1");
        }

        @Test
        @DisplayName("should process template with no variables")
        void should_process_template_with_no_variables() {
            String result = templateService.getContent("[[${key?.value}]], [[${key?.stringValue}]]",
                    Map.of());
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(", ");
        }

        @Test
        @DisplayName("should throw exception with null variables in template")
        void should_throw_exception_with_null_variables_in_template() {
            assertThatThrownBy(() -> templateService.getContent("[[${summary.value}]]", Map.of()))
                    .isInstanceOf(TemplateInputException.class);
        }

        @Test
        @DisplayName("should throw exception when variables is null")
        @SuppressWarnings("ConstantConditions")
        void should_throw_exception_when_variables_is_null() {
            assertThatThrownBy(() -> templateService.getContent(null, Map.of()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> templateService.getContent("", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}