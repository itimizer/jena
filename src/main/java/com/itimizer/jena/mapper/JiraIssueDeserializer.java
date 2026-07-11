package com.itimizer.jena.mapper;

import com.itimizer.jena.domain.JiraIssue;
import com.itimizer.jena.domain.JiraIssueBasicObject;
import com.itimizer.jena.domain.JiraIssueChangelogGroup;
import com.itimizer.jena.domain.JiraIssueField;
import com.itimizer.jena.domain.JiraProject;
import com.itimizer.jena.domain.JiraUser;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.itimizer.jena.domain.JiraIssueFieldId.AFFECTS_VERSIONS_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.ASSIGNEE_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.COMPONENTS_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.CREATED_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.CREATOR_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.DESCRIPTION_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.DUE_DATE_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.FIX_VERSIONS_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.ISSUE_TYPE_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.LABELS_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.ORIGINAL_ESTIMATE_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.PRIORITY_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.PROJECT_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.REMAINING_ESTIMATE_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.REPORTER_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.RESOLUTIONDATE_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.RESOLUTION_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.STATUS_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.SUMMARY_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.TIME_SPENT_FIELD;
import static com.itimizer.jena.domain.JiraIssueFieldId.UPDATED_FIELD;

/**
 * Custom Jackson deserializer for the Jira issue payload. Flattens the nested {@code fields} object
 * into a {@link JiraIssue}, maps known system fields to typed properties, collects
 * {@code customfield_*} values, and carries the {@code names}/{@code changelog} sections used
 * downstream.
 */
@Slf4j
public class JiraIssueDeserializer extends ValueDeserializer<JiraIssue> {

    private static final String FIELD_SECTION = "fields";
    private static final String NAME_SECTION = "names";
    private static final String SCHEMA_SECTION = "schema";
    private static final String CHANGELOG_SECTION = "changelog";
    private static final String HISTORY_SECTION = "histories";

    private static final String JIRA_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter JIRA_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern(JIRA_DATE_TIME_PATTERN);
    private static final DateTimeFormatter JIRA_DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .build();

    @Override
    public JiraIssue deserialize(JsonParser p, DeserializationContext ctxt) {
        var issueKey = "empty";

        try {
            var node = objectMapper.readTree(p);
            issueKey = getIssueStringValue(node, "key").orElse("unknown");

            if (!issueKey.equals("unknown")) {
                MDC.put("issue", issueKey);
            }
            log.debug("Starting deserialization of Jira issue: {}", issueKey);
            log.trace("Jira issue JSON content: {}", node.toPrettyString());

            final var id = Long.parseLong(getIssueStringValue(node, "id").orElse("0L"));
            final var self = getIssueStringValue(node, "self").orElse(null);
            final var summary = getFieldStringValue(node, SUMMARY_FIELD.id).orElse(null);
            final var status = objectMapper
                    .treeToValue(getFieldNode(node, STATUS_FIELD.id), JiraIssueBasicObject.class);
            final var issueType =
                    objectMapper.treeToValue(getFieldNode(node, ISSUE_TYPE_FIELD.id),
                            JiraIssueBasicObject.class);
            final var project = objectMapper
                    .treeToValue(getFieldNode(node, PROJECT_FIELD.id), JiraProject.class);
            final List<JiraIssueBasicObject> components =
                    getFieldNode(node, COMPONENTS_FIELD.id) != null
                    ? objectMapper
                        .treeToValue(getFieldNode(node, COMPONENTS_FIELD.id),
                                new TypeReference<>() {
                                })
                    : null;
            final var description = getFieldStringValue(node, DESCRIPTION_FIELD.id).orElse(null);
            final var creator = getFieldNode(node, CREATOR_FIELD.id) != null
                    ? objectMapper
                        .treeToValue(getFieldNode(node, CREATOR_FIELD.id), JiraUser.class)
                    : null;
            final var reporter = getFieldNode(node, REPORTER_FIELD.id) != null
                    ? objectMapper
                        .treeToValue(getFieldNode(node, REPORTER_FIELD.id), JiraUser.class)
                    : null;
            final var assignee = getFieldNode(node, ASSIGNEE_FIELD.id) != null
                    ? objectMapper
                        .treeToValue(getFieldNode(node, ASSIGNEE_FIELD.id), JiraUser.class)
                    : null;
            final var resolution = getFieldNode(node, RESOLUTION_FIELD.id) != null
                    ? objectMapper
                        .treeToValue(getFieldNode(node, RESOLUTION_FIELD.id),
                                JiraIssueBasicObject.class)
                    : null;
            final var created =
                    parseZonedDateTime(getFieldStringValue(node, CREATED_FIELD.id).orElse(null));
            final var updated =
                    parseZonedDateTime(getFieldStringValue(node, UPDATED_FIELD.id).orElse(null));
            final var resolutiondate =
                    parseZonedDateTime(getFieldStringValue(node, RESOLUTIONDATE_FIELD.id)
                            .orElse(null));
            final var duedate =
                    parseLocalDate(getFieldStringValue(node, DUE_DATE_FIELD.id).orElse(null));
            final var priority = getFieldNode(node, PRIORITY_FIELD.id) != null
                    ? objectMapper
                        .treeToValue(getFieldNode(node, PRIORITY_FIELD.id),
                                JiraIssueBasicObject.class)
                    : null;
            final List<JiraIssueBasicObject> fixVersions =
                    getFieldNode(node, FIX_VERSIONS_FIELD.id) != null
                            ? objectMapper
                            .treeToValue(getFieldNode(node, FIX_VERSIONS_FIELD.id),
                                    new TypeReference<>() {
                                    })
                            : null;
            final List<JiraIssueBasicObject> versions =
                    getFieldNode(node, AFFECTS_VERSIONS_FIELD.id) != null
                            ? objectMapper
                            .treeToValue(getFieldNode(node, AFFECTS_VERSIONS_FIELD.id),
                                    new TypeReference<>() {
                                    })
                            : null;
            final long timeoriginalestimate =
                    getFieldLongValue(node, ORIGINAL_ESTIMATE_FIELD.id).orElse(0L);
            final long timeestimate =
                    getFieldLongValue(node, REMAINING_ESTIMATE_FIELD.id).orElse(0L);
            final long timespent = getFieldLongValue(node, TIME_SPENT_FIELD.id).orElse(0L);
            final Set<String> labels =
                    getFieldNode(node, LABELS_FIELD.id) != null
                            ? objectMapper
                                .treeToValue(getFieldNode(node, LABELS_FIELD.id),
                                        new TypeReference<>() {})
                            : null;
            final var issueFields = getIssueFields(node);
            final List<JiraIssueChangelogGroup> changelog =
                    objectMapper
                            .treeToValue(node.get(CHANGELOG_SECTION).get(HISTORY_SECTION),
                                    new TypeReference<>() {
                                    });
            final var names = getIssueFieldNames(node);

            var jiraIssue = new JiraIssue(
                    id,
                    self,
                    issueKey,
                    status,
                    issueType,
                    project,
                    components,
                    summary,
                    description,
                    creator,
                    reporter,
                    assignee,
                    resolution,
                    created,
                    updated,
                    resolutiondate,
                    duedate,
                    priority,
                    fixVersions,
                    versions,
                    timeoriginalestimate,
                    timeestimate,
                    timespent,
                    labels,
                    issueFields,
                    changelog,
                    names
            );
            log.debug("Successfully deserialized Jira issue: {}", issueKey);
            log.trace("Deserialized Jira issue: {}", jiraIssue);

            return jiraIssue;
        } catch (RuntimeException e) {
            log.error("Failed to deserialize Jira issue: {}", issueKey, e);
            throw e;
        } finally {
            MDC.remove("issue");
        }
    }

    private Optional<String> getIssueStringValue(final JsonNode node, final String fieldName) {
        final var fieldNode = node.get(fieldName);

        if (fieldNode == null) {
            log.debug("Issue field '{}' not found", fieldName);
            return Optional.empty();
        }
        if (fieldNode.isNull()) {
            log.trace("Issue field '{}' exists but is null", fieldName);
            return Optional.empty();
        }
        if (!fieldNode.isValueNode()) {
            log.warn("Issue field '{}' is not a value node (type: {}). "
                            + "Using asText() may not return expected value.",
                    fieldName, fieldNode.getNodeType());
        }

        var value = fieldNode.asString();
        log.trace("Retrieved issue field '{}' with value: '{}'", fieldName, value);

        return Optional.of(value);
    }

    private Optional<String> getFieldStringValue(final JsonNode node, final String fieldName) {
        final var fields = node.get(FIELD_SECTION);

        if (fields == null) {
            log.debug("Fields section '{}' not found while accessing string field: {}",
                    FIELD_SECTION, fieldName);
            return Optional.empty();
        }

        final var fieldNode = fields.get(fieldName);

        if (fieldNode == null) {
            log.debug("String field '{}' not found in fields section", fieldName);
            return Optional.empty();
        }
        if (fieldNode.isNull()) {
            log.trace("String field '{}' exists but is null", fieldName);
            return Optional.empty();
        }
        if (!fieldNode.isValueNode()) {
            log.warn("Field '{}' is not a value node (type: {}). "
                            + "Using asText() may not return expected value.",
                    fieldName, fieldNode.getNodeType());
        }

        var value = fieldNode.asString();
        log.trace("Retrieved string field '{}' with value: '{}'", fieldName, value);

        return Optional.of(value);
    }

    private Optional<Long> getFieldLongValue(final JsonNode node, final String fieldName) {
        final var fields = node.get(FIELD_SECTION);

        if (fields == null) {
            log.debug("Fields section '{}' not found while accessing long field: {}",
                    FIELD_SECTION, fieldName);
            return Optional.empty();
        }

        final var fieldNode = fields.get(fieldName);

        if (fieldNode == null) {
            log.debug("Long field '{}' not found in fields section", fieldName);
            return Optional.empty();
        }
        if (fieldNode.isNull()) {
            log.trace("Long field '{}' exists but is null", fieldName);
            return Optional.empty();
        }
        if (!fieldNode.isValueNode()) {
            log.warn("Field '{}' is not a value node (type: {}). "
                            + "Using asLong() may not return expected value.",
                    fieldName, fieldNode.getNodeType());
        }

        Long value = fieldNode.asLong();
        log.trace("Retrieved long field '{}' with value: '{}'", fieldName, value);

        return Optional.of(value);
    }

    private JsonNode getFieldNode(final JsonNode node, final String fieldName) {
        final var fields = node.get(FIELD_SECTION);

        if (fields == null) {
            log.debug("Fields section '{}' not found in JSON node for field: {}",
                    FIELD_SECTION, fieldName);
            return null;
        }

        final var fieldNode = fields.get(fieldName);

        if (fieldNode == null) {
            log.debug("Field '{}' not found in fields section", fieldName);
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("Retrieved field '{}' with value: {}", fieldName,
                    fieldNode.isValueNode() ? fieldNode.asString() : fieldNode.getNodeType());
        }

        return fieldNode;
    }

    private List<JiraIssueField> getIssueFields(final JsonNode node) {
        final var names = node.get(NAME_SECTION);

        if (names == null || names.isNull()) {
            log.warn("No names section found for issue");
            return Collections.emptyList();
        }

        final var namesMap = parseNames(names);
        final var schema = node.get(SCHEMA_SECTION);

        if (schema == null || schema.isNull()) {
            log.warn("No schema section found for issue");
            return Collections.emptyList();
        }

        final var typesMap = parseTypes(schema);
        final var itemsMap = parseItems(schema);
        final var systemsMap = parseSystems(schema);
        final var customsMap = parseCustoms(schema);
        final var fields = node.get(FIELD_SECTION);

        if (fields == null || fields.isNull()) {
            log.warn("No fields section found for issue");
            return Collections.emptyList();
        }

        final List<JiraIssueField> issueFields = new ArrayList<>(fields.size());

        for (final var fieldName : fields.propertyNames()) {
            if (!fieldName.matches("^customfield_\\d+$")) {
                log.debug("Skipping special field: {}", fieldName);
                continue;
            }

            final var fieldNode = fields.get(fieldName);

            if (fieldNode == null || fieldNode.isNull()) {
                log.debug("Field '{}' is null for issue", fieldName);
            }

            issueFields.add(new JiraIssueField(
                    fieldName,
                    namesMap.get(fieldName),
                    typesMap.get(fieldName),
                    itemsMap.get(fieldName),
                    systemsMap.get(fieldName),
                    customsMap.get(fieldName),
                    fieldNode == null || fieldNode.isNull() ? null : fieldNode
            ));
            log.debug("Processed custom field: {}", fieldName);
        }
        return issueFields;
    }

    private Map<String, String> getIssueFieldNames(final JsonNode node) {
        final var fieldsNames = node.get(NAME_SECTION);

        if (fieldsNames == null || fieldsNames.isNull()) {
            log.warn("No field names section found for issue");
            return Collections.emptyMap();
        }

        final Map<String, String> names = new HashMap<>(fieldsNames.size());

        for (final var fieldName : fieldsNames.propertyNames()) {
            final var fieldNode = fieldsNames.get(fieldName);
            if (fieldNode == null || fieldNode.isNull()) {
                log.debug("Field name '{}' is null for issue", fieldName);
                continue;
            }
            names.put(fieldNode.asString(), fieldName);
            log.debug("Processed field name: {}", fieldName);
        }
        return names;
    }

    private Map<String, String> parseNames(final JsonNode node) {
        if (node == null || node.isNull()) {
            log.debug("Names node is null");
            return Collections.emptyMap();
        }

        final Map<String, String> res = new HashMap<>();

        for (final var key : node.propertyNames()) {
            res.put(key, node.get(key).asString());
        }
        return res;
    }

    private Map<String, String> parseTypes(final JsonNode node) {
        if (node == null || node.isNull()) {
            log.debug("Types node is null");
            return Collections.emptyMap();
        }

        final Map<String, String> res = new HashMap<>();

        for (final var fieldId : node.propertyNames()) {
            var fieldDefinition = node.get(fieldId);
            res.put(fieldId, fieldDefinition.get("type").asString());
        }
        return res;
    }

    private Map<String, String> parseItems(final JsonNode node) {
        if (node == null || node.isNull()) {
            log.debug("Items node is null");
            return Collections.emptyMap();
        }

        final Map<String, String> res = new HashMap<>();

        for (final var fieldId : node.propertyNames()) {
            var fieldDefinition = node.get(fieldId);
            if (fieldDefinition.get("items") != null) {
                res.put(fieldId, fieldDefinition.get("items").asString());
            }
        }
        return res;
    }

    private Map<String, String> parseCustoms(final JsonNode node) {
        if (node == null || node.isNull()) {
            log.debug("Customs node is null");
            return Collections.emptyMap();
        }

        final Map<String, String> res = new HashMap<>();

        for (final var fieldId : node.propertyNames()) {
            var fieldDefinition = node.get(fieldId);
            if (fieldDefinition.get("custom") != null) {
                res.put(fieldId, fieldDefinition.get("custom").asString());
            }

        }
        return res;
    }

    private Map<String, String> parseSystems(final JsonNode node) {
        if (node == null || node.isNull()) {
            log.debug("Systems node is null");
            return Collections.emptyMap();
        }

        final Map<String, String> res = new HashMap<>();

        for (final var fieldId : node.propertyNames()) {
            var fieldDefinition = node.get(fieldId);
            if (fieldDefinition.get("system") != null) {
                res.put(fieldId, fieldDefinition.get("system").asString());
            }
        }
        return res;
    }

    private ZonedDateTime parseZonedDateTime(final String date) {
        if (date == null || date.trim().isEmpty()) {
            log.debug("Input datetime string is null or empty");
            return null;
        }
        return ZonedDateTime.parse(date, JIRA_DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseLocalDate(final String date) {
        if (date == null || date.trim().isEmpty()) {
            log.debug("Input date string is null or empty");
            return null;
        }
        return LocalDate.parse(date, JIRA_DATE_FORMATTER).atStartOfDay();
    }
}
