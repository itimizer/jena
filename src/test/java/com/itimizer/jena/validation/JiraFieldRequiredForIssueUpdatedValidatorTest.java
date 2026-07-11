package com.itimizer.jena.validation;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.entity.Event;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraFieldRequiredForIssueUpdatedValidator Tests")
class JiraFieldRequiredForIssueUpdatedValidatorTest {

    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintValidatorContext
            .ConstraintViolationBuilder violationBuilder;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder
            .NodeBuilderCustomizableContext nodeBuilder;

    private JiraFieldRequiredForIssueUpdatedValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JiraFieldRequiredForIssueUpdatedValidator();
    }

    @Test
    @DisplayName("should return true when DTO is null")
    void should_return_true_when_dto_is_null() {
        boolean result = validator.isValid(null, context);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when event is not ISSUE_CREATED")
    void should_return_true_when_event_is_not_issue_created() {
        RuleCreateDto dto = new RuleCreateDto(
                1L,
                true,
                Event.ISSUE_CREATED,
                null,
                "1",
                "Open",
                "2",
                "Resolved",
                false);

        boolean result = validator.isValid(dto, context);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when event is ISSUE_UPDATED and field is provided")
    void should_return_true_when_event_is_issue_updated_and_field_is_provided() {
        RuleCreateDto dto = new RuleCreateDto(
                1L,
                true,
                Event.ISSUE_UPDATED,
                "status",
                "1",
                "Open",
                "2",
                "Resolved",
                false);

        boolean result = validator.isValid(dto, context);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when event is ISSUE_UPDATED and field is not provided")
    void should_return_false_when_event_is_issue_updated_and_field_is_not_provided() {
        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation())
                .thenReturn(context);

        RuleCreateDto dto = new RuleCreateDto(
                1L,
                true,
                Event.ISSUE_UPDATED,
                null,
                "1",
                "Open",
                "2",
                "Resolved",
                false);

        boolean result = validator.isValid(dto, context);
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ", "   ", "\t", "\n"})
    @DisplayName("should return false when event is ISSUE_UPDATED and field is empty")
    void should_return_false_when_event_is_issue_updated_and_field_is_empty(String field) {
        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation())
                .thenReturn(context);

        RuleCreateDto dto = new RuleCreateDto(
                1L,
                true,
                Event.ISSUE_UPDATED,
                field,
                "1",
                "Open",
                "2",
                "Resolved",
                false);

        boolean result = validator.isValid(dto, context);
        assertThat(result).isFalse();
    }
}