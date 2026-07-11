package com.itimizer.jena.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level bean-validation constraint requiring a rule's {@code field} to be set when its
 * {@code event} is {@code ISSUE_UPDATED}. Implemented by
 * {@link JiraFieldRequiredForIssueUpdatedValidator}.
 */
@Constraint(validatedBy = JiraFieldRequiredForIssueUpdatedValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JiraFieldRequiredForIssueUpdated {

    String message() default "Jira field is required for event ISSUE_UPDATED";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
