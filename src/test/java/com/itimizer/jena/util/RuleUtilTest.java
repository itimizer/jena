package com.itimizer.jena.util;

import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.entity.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleUtil Tests")
class RuleUtilTest {

    private final RuleUtil ruleUtil = new RuleUtil();

    @Test
    @DisplayName("should return true when item matches rule")
    void should_return_true_when_item_matches_rule() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr("Open");
        rule.setTo("2");
        rule.setToStr("In Progress");
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when field is null")
    void should_return_false_when_field_is_null() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField(null);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when field is empty")
    void should_return_false_when_field_is_empty() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("");

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when field does not match")
    void should_return_false_when_field_does_not_match() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("priority");
        rule.setFrom("1");
        rule.setFromStr("Open");
        rule.setTo("2");
        rule.setToStr("In Progress");
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when from value does not match")
    void should_return_false_when_from_value_does_not_match() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("3");
        rule.setFromStr("Open");
        rule.setTo("2");
        rule.setToStr("In Progress");
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when to value does not match")
    void should_return_false_when_to_value_does_not_match() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr("Open");
        rule.setTo("3");
        rule.setToStr("In Progress");
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when hasChanged is true and value did not change")
    void should_return_false_when_has_changed_true_and_no_change() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("1")
                .toString("Open")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr("Open");
        rule.setTo("1");
        rule.setToStr("Open");
        rule.setHasChanged(true);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true when hasChanged is true and value changed")
    void should_return_true_when_has_changed_true_and_value_changed() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr("Open");
        rule.setTo("2");
        rule.setToStr("In Progress");
        rule.setHasChanged(true);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when rule string values are null")
    void should_return_true_when_rule_strings_null() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom(null);
        rule.setFromStr(null);
        rule.setTo(null);
        rule.setToStr(null);
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when rule partial values are null")
    void should_return_true_when_rule_partial_values_null() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr(null);
        rule.setTo("2");
        rule.setToStr(null);
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when fromString does not match")
    void should_return_false_when_from_string_does_not_match() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr("Closed");
        rule.setTo("2");
        rule.setToStr("In Progress");
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return false when toString does not match")
    void should_return_false_when_to_string_does_not_match() {
        JiraIssueChangelogItem item = JiraIssueChangelogItem.builder()
                .field("status")
                .from("1")
                .fromString("Open")
                .to("2")
                .toString("In Progress")
                .build();

        Rule rule = new Rule();
        rule.setField("status");
        rule.setFrom("1");
        rule.setFromStr("Open");
        rule.setTo("2");
        rule.setToStr("Closed");
        rule.setHasChanged(false);

        boolean result = ruleUtil.isItemMatchesRule(item, rule);
        assertThat(result).isFalse();
    }
}
