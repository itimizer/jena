package com.itimizer.jena.util;

import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.entity.Rule;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Decides whether a changelog item satisfies a rule. The field must match, and each of the rule's
 * {@code from}/{@code fromStr}/{@code to}/{@code toStr} regexes (a {@code null} regex means "any")
 * must match the item's corresponding value. When {@code hasChanged} is set, an item whose value
 * did not actually change is rejected.
 */
@Component
public class RuleUtil {

    /**
     * Checks the changelog item against the rule's field and value conditions.
     *
     * @return {@code true} if the changelog item matches the rule's field and value conditions
     */
    public boolean isItemMatchesRule(JiraIssueChangelogItem item, Rule rule) {
        if (rule.getField() == null || rule.getField().isEmpty()) {
            return false;
        }

        if (rule.getField().equals(item.getField())
                && rule.isHasChanged()
                && Objects.equals(item.getFrom(), item.getTo())
                && Objects.equals(item.getFromString(), item.getToString())) {
            return false;
        }

        return rule.getField().equals(item.getField())
                && isStringsMatch(item.getFrom(), rule.getFrom())
                && isStringsMatch(item.getFromString(), rule.getFromStr())
                && isStringsMatch(item.getTo(), rule.getTo())
                && isStringsMatch(item.getToString(), rule.getToStr());
    }

    private boolean isStringsMatch(String itemString, String ruleString) {
        if (ruleString == null) {
            return true;
        }
        return Pattern.matches(ruleString, Objects.requireNonNullElse(itemString, "null"));
    }
}
