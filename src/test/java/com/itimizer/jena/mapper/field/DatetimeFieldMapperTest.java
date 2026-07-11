package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DateTimeFieldMapper Tests")
class DatetimeFieldMapperTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Jira jira;
    @Mock
    private ApplicationProperties.Jira.Formatter formatter;

    private DatetimeFieldMapper datetimeFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        datetimeFieldMapper = new DatetimeFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = datetimeFieldMapper.toValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should convert datetime field to template field value")
        void should_convert_datetime_field_to_template_field_value() {
            String jiraDatetime = "2026-01-01T01:01:01.000+0000";
            JsonNode jsonNode = objectMapper.valueToTree(jiraDatetime);

            String result = datetimeFieldMapper.toValue(jsonNode);
            assertThat(result).isEqualTo("2026-01-01T01:01:01+0000");
        }

        @Test
        @DisplayName("should throw exception for invalid datetime format")
        void should_throw_exception_for_invalid_datetime_format() {
            JsonNode invalidDatetime = objectMapper.valueToTree("2026/01/01 00:00:00");

            assertThatThrownBy(() -> datetimeFieldMapper.toValue(invalidDatetime))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("should throw exception for non-datetime string")
        void should_throw_exception_for_non_datetime_string() {
            JsonNode nonDate = objectMapper.valueToTree("not-a-datetime");

            assertThatThrownBy(() -> datetimeFieldMapper.toValue(nonDate))
                    .isInstanceOf(DateTimeParseException.class);
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = datetimeFieldMapper.toStringValue(null);
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @CsvSource({
            "dd.MM.yyyy HH:mm:ss Z, 01.01.2026 01:01:01 +0000",
            "dd.MM.yyyy HH:mm, 01.01.2026 01:01",
            "yyyy-MM-dd HH:mm:ss, 2026-01-01 01:01:01",
            "MM/dd/yyyy hh:mm a, 01/01/2026 01:01 AM",
            "EEEE MMMM d yyyy HH:mm, Thursday January 1 2026 01:01",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ, 2026-01-01T01:01:01.000+0000"
        })
        @DisplayName("should convert datetime field to template field string value")
        void should_convert_datetime_field_to_template_field_string_value(String pattern,
                                                                          String expectedPrefix) {
            datetimeFieldMapper.setApplicationProperties(applicationProperties);
            when(applicationProperties.getJira()).thenReturn(jira);
            when(jira.getFormatter()).thenReturn(formatter);
            when(formatter.getDatetimePattern()).thenReturn(pattern);

            String jiraDatetime = "2026-01-01T01:01:01.000+0000";
            JsonNode jsonNode = objectMapper.valueToTree(jiraDatetime);

            String result = datetimeFieldMapper.toStringValue(jsonNode);
            assertThat(result).isEqualTo(expectedPrefix);
        }

        @Test
        @DisplayName("should throw exception for invalid datetime format")
        void should_throw_exception_for_invalid_datetime_format() {
            JsonNode invalidDate = objectMapper.valueToTree("2026/01/01 00:00:00");

            assertThatThrownBy(() -> datetimeFieldMapper.toStringValue(invalidDate))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("should throw exception for non-datetime string")
        void should_throw_exception_for_non_datetime_string() {
            JsonNode nonDate = objectMapper.valueToTree("not-a-date");

            assertThatThrownBy(() -> datetimeFieldMapper.toStringValue(nonDate))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("should throw exception for invalid formatter")
        void should_throw_exception_for_invalid_formatter() {
            datetimeFieldMapper.setApplicationProperties(applicationProperties);
            when(applicationProperties.getJira()).thenReturn(jira);
            when(jira.getFormatter()).thenReturn(formatter);
            when(formatter.getDatetimePattern()).thenReturn("not-a-formatter");

            String jiraDatetime = "2026-01-01T01:01:01.000+0000";
            JsonNode jsonNode = objectMapper.valueToTree(jiraDatetime);

            assertThatThrownBy(() -> datetimeFieldMapper.toStringValue(jsonNode))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}