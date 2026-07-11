package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.itimizer.jena.config.ApplicationProperties;
import com.itimizer.jena.domain.JiraIssueField;
import com.itimizer.jena.domain.TemplateField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FieldMapper Tests")
class FieldMapperTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Jira jira;
    @Mock
    private ApplicationProperties.Jira.Formatter formatter;

    private FieldMapper fieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fieldMapper = new FieldMapper(applicationProperties);
    }

    @Nested
    @DisplayName("convertToTemplateField() with null value and unknown mapper")
    class ConvertToTemplateFieldTests {

        @Test
        @DisplayName("should return empty TemplateField when field value is null")
        void should_return_empty_template_field_when_field_value_is_null() {
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Custom Field",
                    "string",
                    null,
                    null,
                    "unknown_field_type",
                    null
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isNull();
            assertThat(result.getStringValue()).isNull();
        }

        @Test
        @DisplayName("should return TemplateField with only string value when no mapper found")
        void should_return_template_field_with_only_string_value_when_no_mapper_found() {
            JsonNode textValue = new ObjectMapper().valueToTree("Simple text value");
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Custom Field",
                    "string",
                    null,
                    null,
                    "unknown_field_type",
                    textValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isNull();
            assertThat(result.getStringValue()).isEqualTo("Simple text value");
        }
    }

    @Nested
    @DisplayName("TextField and TextArea tests")
    class TextFieldTests {

        @Test
        @DisplayName("should handle text field with StringFieldMapper")
        void should_handle_text_field_with_string_field_mapper() {
            JsonNode textValue = new ObjectMapper().valueToTree("Simple text value");
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Text Field",
                    "string",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:textfield",
                    textValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isNull();
            assertThat(result.getStringValue()).isEqualTo("Simple text value");
        }

        @Test
        @DisplayName("should handle text area with StringFieldMapper")
        void should_handle_text_area_with_string_field_mapper() {
            JsonNode textValue = new ObjectMapper().valueToTree("Multi-line\ntext\nvalue");
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Text Area",
                    "string",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:textarea",
                    textValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isNull();
            assertThat(result.getStringValue()).isEqualTo("Multi-line\ntext\nvalue");
        }
    }

    @Nested
    @DisplayName("Option and Select tests")
    class OptionFieldTests {

        @Test
        @DisplayName("should handle select field with OptionFieldMapper")
        void should_handle_select_field_with_option_field_mapper() {
            ObjectNode optionValue = objectMapper.createObjectNode()
                    .put("id", "10000")
                    .put("value", "Option 1");

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Select Field",
                    "option",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:select",
                    optionValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("10000");
            assertThat(result.getStringValue()).isEqualTo("Option 1");
        }

        @Test
        @DisplayName("should handle radio button with OptionFieldMapper")
        void should_handle_radio_button_with_option_field_mapper() {
            ObjectNode radioValue = objectMapper.createObjectNode()
                    .put("id", "10000")
                    .put("value", "Radio Option");

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Radio Field",
                    "option",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons",
                    radioValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("10000");
            assertThat(result.getStringValue()).isEqualTo("Radio Option");
        }
    }

    @Nested
    @DisplayName("Multi-Select and Checkbox tests")
    class MultiSelectFieldTests {

        @Test
        @DisplayName("should handle multi-select with MultiSelectFieldMapper")
        void should_handle_multi_select_with_multi_select_field_mapper() {
            ArrayNode multiSelectValue = objectMapper.createArrayNode();

            multiSelectValue.add(objectMapper.createObjectNode()
                    .put("id", "10001")
                    .put("value", "Option 1"));

            multiSelectValue.add(objectMapper.createObjectNode()
                    .put("id", "10002")
                    .put("value", "Option 2"));

            multiSelectValue.add(objectMapper.createObjectNode()
                    .put("id", "10003")
                    .put("value", "Option 3"));

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Multi-Select Field",
                    "array",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:multiselect",
                    multiSelectValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("[10001, 10002, 10003]");
            assertThat(result.getStringValue()).isEqualTo("Option 1, Option 2, Option 3");
        }

        @Test
        @DisplayName("should handle checkboxes with MultiSelectFieldMapper")
        void shouldHandleCheckboxes() {
            ArrayNode checkboxValue = objectMapper.createArrayNode();

            checkboxValue.add(objectMapper.createObjectNode()
                    .put("id", "10001")
                    .put("value", "Check 1"));

            checkboxValue.add(objectMapper.createObjectNode()
                    .put("id", "10002")
                    .put("value", "Check 2"));

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Checkbox Field",
                    "array",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:multicheckboxes",
                    checkboxValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("[10001, 10002]");
            assertThat(result.getStringValue()).isEqualTo("Check 1, Check 2");
        }

        @Test
        @DisplayName("should handle empty multi-select")
        void should_handle_empty_multi_select() {
            ArrayNode emptyValue = objectMapper.createArrayNode();
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Multi-Select Field",
                    "array",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:multiselect",
                    emptyValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("[]");
            assertThat(result.getStringValue()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Date and DateTime tests")
    class DateFieldTests {

        @BeforeEach
        void setUp() {
            when(applicationProperties.getJira()).thenReturn(jira);
            when(jira.getFormatter()).thenReturn(formatter);
        }

        @Test
        @DisplayName("should handle date field with DateFieldMapper")
        void should_handle_date_field_with_date_field_mapper() {
            when(formatter.getDatePattern()).thenReturn("dd.MM.yyyy");
            JsonNode dateValue = new ObjectMapper().valueToTree("2026-01-01");
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Date Field",
                    "date",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:datepicker",
                    dateValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("2026-01-01");
            assertThat(result.getStringValue()).isEqualTo("01.01.2026");
        }

        @Test
        @DisplayName("Should handle datetime field with DatetimeFieldMapper")
        void should_handle_datetime_field_with_datetime_field_mapper() {
            when(formatter.getDatetimePattern()).thenReturn("dd.MM.yyyy HH:mm");
            JsonNode datetimeValue = new ObjectMapper().valueToTree("2026-01-01T01:01:01.000+0000");
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Datetime Field",
                    "datetime",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:datetime",
                    datetimeValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("2026-01-01T01:01:01+0000");
            assertThat(result.getStringValue()).isEqualTo("01.01.2026 01:01");
        }
    }

    @Nested
    @DisplayName("Number field tests")
    class NumberFieldTests {

        @Test
        @DisplayName("Should handle float field with StringFieldMapper")
        void should_handle_float_field_with_string_field_mapper() {
            JsonNode numberValue = new ObjectMapper().valueToTree(123.45);
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Float Field",
                    "number",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:float",
                    numberValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isNull();
            assertThat(result.getStringValue()).isEqualTo("123.45");
        }
    }

    @Nested
    @DisplayName("User picker tests")
    class UserFieldTests {

        @Test
        @DisplayName("should handle single user picker with UserFieldMapper")
        void should_handle_single_user_picker_with_user_field_mapper() {
            ObjectNode userValue = objectMapper.createObjectNode()
                    .put("key", "john.doe")
                    .put("displayName", "John Doe");

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "User Picker",
                    "user",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:userpicker",
                    userValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("john.doe");
            assertThat(result.getStringValue()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should handle multi-user picker with MultiUserFieldMapper")
        void should_handle_multi_user_picker_with_multi_user_field_mapper() {
            ArrayNode multiUserValue = objectMapper.createArrayNode();

            ObjectNode user1 = objectMapper.createObjectNode()
                    .put("key", "john.doe")
                    .put("displayName", "John Doe");
            multiUserValue.add(user1);

            ObjectNode user2 = objectMapper.createObjectNode()
                    .put("key", "jane.smith")
                    .put("displayName", "Jane Smith");
            multiUserValue.add(user2);

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Multi User Picker",
                    "array",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker",
                    multiUserValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("[john.doe, jane.smith]");
            assertThat(result.getStringValue()).isEqualTo("John Doe, Jane Smith");
        }

        @Test
        @DisplayName("Should handle empty multi-user picker")
        void should_handle_empty_multi_user_picker() {
            ArrayNode emptyValue = objectMapper.createArrayNode();
            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Multi User Picker",
                    "array",
                    null,
                    null,
                    "com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker",
                    emptyValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("[]");
            assertThat(result.getStringValue()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Insight field tests")
    class InsightFieldTests {

        @Test
        @DisplayName("should handle insight field with InsightFieldMapper")
        void should_handle_insight_field_with_insight_field_mapper() {
            ArrayNode insightValue = objectMapper.createArrayNode();
            insightValue.add("Insight 1 (CI-1)");
            insightValue.add("Insight 2 (CI-2)");

            JiraIssueField field = new JiraIssueField(
                    "customfield_10000",
                    "Insight Field",
                    "any",
                    null,
                    null,
                    "com.riadalabs.jira.plugins.insight:rlabs-customfield-default-object",
                    insightValue
            );

            TemplateField result = fieldMapper.convertToTemplateField(field);
            assertThat(result).isNotNull();
            assertThat(result.getValue()).isEqualTo("[CI-1, CI-2]");
            assertThat(result.getStringValue()).isEqualTo("Insight 1, Insight 2");
        }
    }
}