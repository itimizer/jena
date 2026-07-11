package com.itimizer.jena.mapper.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptionFieldMapper Tests")
class OptionFieldMapperTest {

    private OptionFieldMapper optionFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        optionFieldMapper = new OptionFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            assertThat(optionFieldMapper.toValue(null)).isNull();
        }

        @Test
        @DisplayName("should extract ID from option object")
        void should_extract_id_from_option_object() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", "10000");
            optionNode.put("value", "Option 1");

            String result = optionFieldMapper.toValue(optionNode);
            assertThat(result).isEqualTo("10000");
        }

        @Test
        @DisplayName("should handle missing id field")
        void sholud_handle_missing_id_field() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("value", "Option 1");

            String result = optionFieldMapper.toValue(optionNode);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle null id field")
        void should_handle_null_id_field() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.putNull("id");
            optionNode.put("value", "Option 1");

            String result = optionFieldMapper.toValue(optionNode);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle empty id field")
        void should_handle_empty_id_field() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", "");
            optionNode.put("value", "Option 1");

            String result = optionFieldMapper.toValue(optionNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle non-object JSON node")
        void should_handle_non_object_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an object");

            String result = optionFieldMapper.toValue(textNode);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            assertThat(optionFieldMapper.toStringValue(null)).isEqualTo("");
        }

        @Test
        @DisplayName("should extract value from option object")
        void should_extract_value_from_option_object() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", "10000");
            optionNode.put("value", "Option 1");

            String result = optionFieldMapper.toStringValue(optionNode);
            assertThat(result).isEqualTo("Option 1");
        }

        @Test
        @DisplayName("should handle missing value field")
        void should_handle_missing_value_field() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", "10000");

            String result = optionFieldMapper.toStringValue(optionNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle null value field")
        void should_handle_null_value_field() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", "10000");
            optionNode.putNull("value");

            String result = optionFieldMapper.toStringValue(optionNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle empty value field")
        void should_handle_empty_value_field() {
            ObjectNode optionNode = objectMapper.createObjectNode();
            optionNode.put("id", "10000");
            optionNode.put("value", "");

            String result = optionFieldMapper.toStringValue(optionNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle non-object JSON node")
        void should_handle_non_object_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an object");

            String result = optionFieldMapper.toStringValue(textNode);
            assertThat(result).isEqualTo("");
        }
    }
}