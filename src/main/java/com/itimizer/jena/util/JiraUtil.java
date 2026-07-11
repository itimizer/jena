package com.itimizer.jena.util;

import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueBasicObject;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.domain.JiraIssueChangelogItem;
import com.itimizer.jena.domain.TemplateField;
import com.itimizer.jena.exception.ValidationException;
import com.itimizer.jena.mapper.field.FieldMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core helpers for turning Jira API data into template-ready form: building the search JQL, slicing
 * the changelog to a time window, resolving changelog field labels to ids, and assembling the field
 * context maps ({@code key} → {@link TemplateField}) that templates render against. The
 * {@code SPECIAL_FIELDS} (versions/components) only carry their new value per change, so their full
 * history is reconstructed by replaying the changelog.
 */
@Component
@RequiredArgsConstructor
public class JiraUtil {

    private static final DateTimeFormatter DATETIME_FORMATTER_LONG =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DUE_DATE_FORMATTER_LONG =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
    private static final DateTimeFormatter DUE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> SPECIAL_FIELDS =
            Set.of("fixVersions", "versions", "components");

    private final ApplicationProperties applicationProperties;

    private final StringUtil stringUtil;
    private final FieldMapper fieldMapper;

    /** Wraps the filter's base JQL with the incremental {@code updated >=} window. */
    public String getJiraSearchJql(final String baseJql, final LocalDateTime updated) {
        if (updated == null) {
            return "";
        }
        if (baseJql == null || baseJql.isBlank()) {
            throw new ValidationException("Filter JQL must not be blank; refusing to build an "
                    + "unbounded search that would match every updated issue.");
        }
        return "(%s) and updated >= '%s'".formatted(
                baseJql,
                updated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    public List<JiraIssueChangelogGroup> getJiraIssueChangelogGroups(final JiraIssue jiraIssue,
                                                                     final ZonedDateTime updated) {
        if (jiraIssue == null) {
            return new ArrayList<>();
        }
        if (updated == null) {
            return jiraIssue.getChangelog();
        }
        var changelog = jiraIssue.getChangelog();
        List<JiraIssueChangelogGroup> jiraIssueChangelogGroups = new ArrayList<>();

        for (var history : changelog) {
            if (!history.getCreated().isBefore(updated)) {
                jiraIssueChangelogGroups.add(history);
            }
        }
        return jiraIssueChangelogGroups;
    }

    public Map<String, Object> getJiraIssueFieldsCtx(final JiraIssue jiraIssue,
                                                     final boolean latest) {
        if (jiraIssue == null) {
            return new HashMap<>();
        }
        var customNames = applicationProperties.getJira().getReversedNames();
        var templateContext = getJiraIssueFieldsLatestCtx(jiraIssue);

        if (!latest) {
            for (var changelogGroup : jiraIssue.getChangelog().reversed()) {
                for (var changelogItem : changelogGroup.getItems()) {
                    String fieldKey;

                    if (!customNames.isEmpty()
                            && customNames.containsKey(changelogItem.getField())) {
                        fieldKey = customNames.get(changelogItem.getField());
                    } else {
                        fieldKey = changelogItem.getFieldtype().equals("custom")
                                ? jiraIssue.getNames().get(changelogItem.getField())
                                : changelogItem.getField();
                    }
                    if (templateContext.containsKey(fieldKey)) {
                        var templateField = (TemplateField) templateContext.get(fieldKey);
                        if (SPECIAL_FIELDS.contains(fieldKey)) {
                            var value = templateField.getValue();
                            var stringValue = templateField.getStringValue();
                            templateField.setValue(getChangedSpecialFieldValue(value,
                                    changelogItem, false));
                            templateField.setStringValue(getChangedSpecialFieldValue(stringValue,
                                    changelogItem, true));
                        } else {
                            templateField.setValue(changelogItem.getFrom());
                            templateField.setStringValue(changelogItem.getFromString());
                        }
                    }
                }
            }
        }

        return templateContext;
    }

    public Map<String, Object> getJiraIssueFieldsCtx(final JiraIssue jiraIssue,
                                                     final long historyId) {
        if (jiraIssue == null) {
            return new HashMap<>();
        }
        var customNames = applicationProperties.getJira().getReversedNames();
        var templateContext = getJiraIssueFieldsLatestCtx(jiraIssue);

        for (var changelogGroup : jiraIssue.getChangelog()) {
            if (changelogGroup.getId() <= historyId) {
                for (var changelogItem : changelogGroup.getItems()) {
                    String fieldKey;

                    if (!customNames.isEmpty()
                            && customNames.containsKey(changelogItem.getField())) {
                        fieldKey = customNames.get(changelogItem.getField());
                    } else {
                        fieldKey = changelogItem.getFieldtype().equals("custom")
                                ? jiraIssue.getNames().get(changelogItem.getField())
                                : changelogItem.getField();
                    }
                    if (templateContext.containsKey(fieldKey)
                            && !SPECIAL_FIELDS.contains(fieldKey)) {
                        var templateField = (TemplateField) templateContext.get(fieldKey);
                        templateField.setValue(changelogItem.getTo());
                        templateField.setStringValue(changelogItem.getToString());
                    }
                }
            } else {
                break;
            }
        }

        for (var changelogGroup : jiraIssue.getChangelog().reversed()) {
            if (changelogGroup.getId() > historyId) {
                for (var changelogItem : changelogGroup.getItems()) {
                    String fieldKey;

                    if (!customNames.isEmpty()
                            && customNames.containsKey(changelogItem.getField())) {
                        fieldKey = customNames.get(changelogItem.getField());
                    } else {
                        fieldKey = changelogItem.getFieldtype().equals("custom")
                                ? jiraIssue.getNames().get(changelogItem.getField())
                                : changelogItem.getField();
                    }
                    if (templateContext.containsKey(fieldKey)) {
                        var templateField = (TemplateField) templateContext.get(fieldKey);
                        if (SPECIAL_FIELDS.contains(fieldKey)) {
                            var value = templateField.getValue();
                            var stringValue = templateField.getStringValue();
                            templateField.setValue(getChangedSpecialFieldValue(value,
                                    changelogItem, false));
                            templateField.setStringValue(getChangedSpecialFieldValue(stringValue,
                                    changelogItem, true));
                        } else {
                            templateField.setValue(changelogItem.getFrom());
                            templateField.setStringValue(changelogItem.getFromString() == null
                                    ? "" : changelogItem.getFromString());
                        }
                    }
                }
            } else {
                break;
            }
        }

        return templateContext;
    }

    public boolean isIssueCreatedAfter(final JiraIssue jiraIssue, final ZonedDateTime after) {
        if (jiraIssue.getCreated() == null) {
            return false;
        }
        return after.isBefore(jiraIssue.getCreated());
    }

    /**
     * Returns a copy of the changelog item with its human field label replaced by the Jira field id
     * (resolved via the issue's {@code names} map), so it can be matched against a rule's
     * {@code field}.
     */
    public JiraIssueChangelogItem convertFieldNameToKey(@NonNull JiraIssueChangelogItem item,
                                                        @NonNull Map<String, String> names) {
        JiraIssueChangelogItem convertedItem;
        var customNames = applicationProperties.getJira().getReversedNames();
        String fieldValue;

        if (!customNames.isEmpty() && customNames.containsKey(item.getField())) {
            fieldValue = customNames.get(item.getField());
        } else if (!names.isEmpty() && names.containsKey(item.getField())) {
            fieldValue = names.get(item.getField());
        } else {
            fieldValue = null;
        }

        if (fieldValue != null) {
            convertedItem = JiraIssueChangelogItem.builder()
                    .field(fieldValue)
                    .from(item.getFrom())
                    .fromString(item.getFromString())
                    .to(item.getTo())
                    .toString(item.getToString())
                    .build();
        } else {
            convertedItem = item;
        }
        return convertedItem;
    }

    public Map<String, Object> getJiraIssueFieldsLatestCtx(final JiraIssue jiraIssue) {
        if (jiraIssue == null) {
            return new HashMap<>();
        }
        var templateContext = getJiraIssueSystemFieldsCtx(jiraIssue);

        for (var customField : jiraIssue.getIssueFields()) {
            if (customField.getKey().matches("^customfield_\\d+$")) {
                templateContext.put(customField.getKey(),
                        fieldMapper.convertToTemplateField(customField));
            }
        }

        return templateContext;
    }

    private Map<String, Object> getJiraIssueSystemFieldsCtx(final JiraIssue jiraIssue) {
        if (jiraIssue == null) {
            return new HashMap<>();
        }
        Map<String, Object> templateContext = new HashMap<>(jiraIssue.getNames().size() + 2);

        templateContext.put("baseUrl", new TemplateField(
                stringUtil.getHostUrl(jiraIssue.getSelf()),
                stringUtil.getBaseUrl(jiraIssue.getSelf())));
        templateContext.put("key", new TemplateField(
                String.valueOf(jiraIssue.getId()),
                jiraIssue.getKey()));
        templateContext.put("status", new TemplateField(
                String.valueOf(jiraIssue.getStatus().getId()),
                jiraIssue.getStatus().getName()));
        templateContext.put("issuetype", new TemplateField(
                String.valueOf(jiraIssue.getIssueType().getId()),
                jiraIssue.getIssueType().getName()));
        templateContext.put("project", new TemplateField(
                String.valueOf(jiraIssue.getProject().getId()),
                jiraIssue.getProject().getName()));
        templateContext.put("components", convertJiraIssueBasicObjects(jiraIssue.getComponents()));
        templateContext.put("summary", new TemplateField(
                null,
                jiraIssue.getSummary()));
        templateContext.put("description", new TemplateField(
                null,
                jiraIssue.getDescription()));
        templateContext.put("creator", jiraIssue.getCreator() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getCreator().getKey(),
                    jiraIssue.getCreator().getDisplayName()));
        templateContext.put("reporter", jiraIssue.getReporter() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getReporter().getKey(),
                    jiraIssue.getReporter().getDisplayName()));
        templateContext.put("assignee", jiraIssue.getAssignee() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getAssignee().getKey(),
                    jiraIssue.getAssignee().getDisplayName()));
        templateContext.put("resolution", jiraIssue.getResolution() == null
                ? new TemplateField()
                : new TemplateField(
                    String.valueOf(jiraIssue.getResolution().getId()),
                    jiraIssue.getResolution().getName()));
        templateContext.put("created", jiraIssue.getCreated() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getCreated().format(DATETIME_FORMATTER_LONG),
                    jiraIssue.getCreated().format(DATETIME_FORMATTER)));
        templateContext.put("updated", jiraIssue.getUpdated() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getUpdated().format(DATETIME_FORMATTER_LONG),
                    jiraIssue.getUpdated().format(DATETIME_FORMATTER)));
        templateContext.put("resolutiondate", jiraIssue.getResolutiondate() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getResolutiondate().format(DATETIME_FORMATTER_LONG),
                    jiraIssue.getResolutiondate().format(DATETIME_FORMATTER)));
        templateContext.put("duedate", jiraIssue.getDuedate() == null
                ? new TemplateField()
                : new TemplateField(
                    jiraIssue.getDuedate().format(DUE_DATE_FORMATTER),
                    jiraIssue.getDuedate().format(DUE_DATE_FORMATTER_LONG)));
        templateContext.put("priority", jiraIssue.getPriority() == null
                ? new TemplateField()
                : new TemplateField(
                    String.valueOf(jiraIssue.getPriority().getId()),
                    jiraIssue.getPriority().getName()));
        templateContext.put("fixVersions",
                convertJiraIssueBasicObjects(jiraIssue.getFixVersions()));
        templateContext.put("versions",
                convertJiraIssueBasicObjects(jiraIssue.getVersions()));
        templateContext.put("timeoriginalestimate", jiraIssue.getTimeoriginalestimate() == 0
                ? new TemplateField()
                : new TemplateField(
                    String.valueOf(jiraIssue.getTimeoriginalestimate()),
                    String.valueOf(jiraIssue.getTimeoriginalestimate())));
        templateContext.put("timeestimate", jiraIssue.getTimeestimate() == 0
                ? new TemplateField()
                : new TemplateField(
                    String.valueOf(jiraIssue.getTimeestimate()),
                    String.valueOf(jiraIssue.getTimeestimate())));
        templateContext.put("timespent", jiraIssue.getTimespent() == 0
                ? new TemplateField()
                : new TemplateField(
                    String.valueOf(jiraIssue.getTimespent()),
                    String.valueOf(jiraIssue.getTimespent())));
        templateContext.put("labels", jiraIssue.getLabels() == null
                ? new TemplateField()
                : new TemplateField(
                    null,
                    String.join(" ", jiraIssue.getLabels())));

        return templateContext;
    }

    private String getChangedSpecialFieldValue(String field,
                                               JiraIssueChangelogItem changelogItem,
                                               boolean isStringValue) {
        if (changelogItem == null) {
            return "";
        }

        if (isStringValue) {
            if (changelogItem.getFromString() == null) {
                return stringUtil.removeFieldChangelogItem(field, changelogItem.getToString());
            } else {
                return stringUtil.addFieldChangelogItem(field, changelogItem.getFromString());
            }
        } else {
            if (changelogItem.getFrom() == null) {
                var value = stringUtil.removeFieldChangelogItem(field, changelogItem.getTo());
                if (value == null || value.isEmpty()) {
                    return null;
                }
                return value;
            } else {
                return stringUtil.addFieldChangelogItem(field, changelogItem.getFrom());
            }
        }
    }

    private TemplateField convertJiraIssueBasicObjects(
            List<JiraIssueBasicObject> jiraIssueBasicObjects) {
        if (jiraIssueBasicObjects == null || jiraIssueBasicObjects.isEmpty()) {
            return new TemplateField();
        }
        var separator = ", ";
        var values = new StringBuilder();
        var stringValues = new StringBuilder();

        for (var obj : jiraIssueBasicObjects) {
            if (!values.isEmpty()) {
                values.append(separator);
            }
            if (!stringValues.isEmpty()) {
                stringValues.append(separator);
            }
            values.append(obj.getId());
            stringValues.append(obj.getName());
        }
        return new TemplateField(values.toString(), stringValues.toString());
    }
}
