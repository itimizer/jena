package com.itimizer.jena.mapper.field;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.itimizer.jena.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DateFieldMapper Tests")
class DateFieldMapperTest {

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ApplicationProperties.Jira jira;
    @Mock
    private ApplicationProperties.Jira.Formatter formatter;

    private DateFieldMapper dateFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dateFieldMapper = new DateFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = dateFieldMapper.toValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should convert date field to template field value")
        void should_convert_date_field_to_template_field_value() {
            String isoDate = "2026-01-01";
            JsonNode jsonNode = objectMapper.valueToTree(isoDate);

            String result = dateFieldMapper.toValue(jsonNode);
            assertThat(result).isEqualTo("2026-01-01");
        }

        @Test
        @DisplayName("should throw exception for invalid date format")
        void should_throw_exception_for_invalid_date_format() {
            JsonNode invalidDate = objectMapper.valueToTree("2026/01/01");

            assertThatThrownBy(() -> dateFieldMapper.toValue(invalidDate))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("should throw exception for non-date string")
        void should_throw_exception_for_non_date_string() {
            JsonNode nonDate = objectMapper.valueToTree("not-a-date");

            assertThatThrownBy(() -> dateFieldMapper.toValue(nonDate))
                    .isInstanceOf(DateTimeParseException.class);
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = dateFieldMapper.toStringValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should convert date field to template field string value")
        void should_convert_date_field_to_template_field_string_value() {
            dateFieldMapper.setApplicationProperties(applicationProperties);
            when(applicationProperties.getJira()).thenReturn(jira);
            when(jira.getFormatter()).thenReturn(formatter);

            String isoDate = "2026-01-01";
            JsonNode jsonNode = objectMapper.valueToTree(isoDate);

            when(formatter.getDatePattern()).thenReturn("dd.MM.yyyy");
            assertThat(dateFieldMapper.toStringValue(jsonNode)).isEqualTo("01.01.2026");

            when(formatter.getDatePattern()).thenReturn("yyyy/MM/dd");
            assertThat(dateFieldMapper.toStringValue(jsonNode)).isEqualTo("2026/01/01");

            when(formatter.getDatePattern()).thenReturn("dd-MM-yyyy");
            assertThat(dateFieldMapper.toStringValue(jsonNode)).isEqualTo("01-01-2026");

            when(formatter.getDatePattern()).thenReturn("MM/dd/yyyy");
            assertThat(dateFieldMapper.toStringValue(jsonNode)).isEqualTo("01/01/2026");

            when(formatter.getDatePattern()).thenReturn("dd MMM yyyy");
            assertThat(dateFieldMapper.toStringValue(jsonNode)).isEqualTo("01 Jan 2026");

            when(formatter.getDatePattern()).thenReturn("EEEE, dd MMMM yyyy");
            assertThat(dateFieldMapper.toStringValue(jsonNode))
                    .isEqualTo("Thursday, 01 January 2026");
        }

        @Test
        @DisplayName("should throw exception for invalid date format")
        void should_throw_exception_for_invalid_date_format() {
            JsonNode invalidDate = objectMapper.valueToTree("2026/01/01");

            assertThatThrownBy(() -> dateFieldMapper.toStringValue(invalidDate))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("should throw exception for non-date string")
        void should_throw_exception_for_non_date_string() {
            JsonNode nonDate = objectMapper.valueToTree("not-a-date");

            assertThatThrownBy(() -> dateFieldMapper.toStringValue(nonDate))
                    .isInstanceOf(DateTimeParseException.class);
        }

        @Test
        @DisplayName("should throw exception for invalid formatter")
        void should_throw_exception_for_invalid_formatter() {
            dateFieldMapper.setApplicationProperties(applicationProperties);
            when(applicationProperties.getJira()).thenReturn(jira);
            when(jira.getFormatter()).thenReturn(formatter);
            when(formatter.getDatePattern()).thenReturn("not-a-formatter");

            String isoDate = "2026-01-01";
            JsonNode jsonNode = objectMapper.valueToTree(isoDate);

            assertThatThrownBy(() -> dateFieldMapper.toStringValue(jsonNode))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}