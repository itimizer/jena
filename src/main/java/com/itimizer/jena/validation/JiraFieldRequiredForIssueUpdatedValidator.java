package com.itimizer.jena.validation;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.entity.Event;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a rule DTO sets {@code field} when its {@code event} is {@code ISSUE_UPDATED}
 * (a field change must name the field to watch). Other events pass without a field.
 */
public class JiraFieldRequiredForIssueUpdatedValidator
        implements ConstraintValidator<JiraFieldRequiredForIssueUpdated, Object> {

    @Override
    public boolean isValid(Object dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        Event event = null;
        String field = null;

        if (dto instanceof RuleCreateDto createDto) {
            event = createDto.event();
            field = createDto.field();
        } else if (dto instanceof RuleDto ruleDto) {
            event = ruleDto.event();
            field = ruleDto.field();
        }

        if (event == Event.ISSUE_UPDATED) {
            var isValid = field != null && !field.trim().isEmpty();

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "Jira field is required when event is ISSUE_UPDATED")
                        .addPropertyNode("field")
                        .addConstraintViolation();
            }

            return isValid;
        }

        return true;
    }
}
