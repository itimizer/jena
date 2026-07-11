package com.itimizer.jena.mapper.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiSelectFieldMapper Tests")
class MultiSelectFieldMapperTest {

    private MultiSelectFieldMapper multiSelectFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        multiSelectFieldMapper = new MultiSelectFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = multiSelectFieldMapper.toValue(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should extract ID from single option")
        void should_extract_id_from_single_option() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option = objectMapper.createObjectNode();
            option.put("id", "10000");
            option.put("value", "Option 1");
            arrayNode.add(option);

            String result = multiSelectFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[10000]");
        }

        @Test
        @DisplayName("should extract IDs from multiple options")
        void should_extract_ids_from_multiple_options() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option1 = objectMapper.createObjectNode();
            option1.put("id", "abc123");
            option1.put("value", "Option 1");
            arrayNode.add(option1);
            ObjectNode option2 = objectMapper.createObjectNode();
            option2.put("id", "10001");
            option2.put("value", "Option 2");
            arrayNode.add(option2);
            ObjectNode option3 = objectMapper.createObjectNode();
            option3.put("id", "10002");
            option3.put("value", "Option 3");
            arrayNode.add(option3);

            String result = multiSelectFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[abc123, 10001, 10002]");
        }

        @Test
        @DisplayName("should handle empty array")
        void should_handle_empty_array() {
            ArrayNode emptyArray = objectMapper.createArrayNode();

            String result = multiSelectFieldMapper.toValue(emptyArray);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle non-array JSON node")
        void should_handle_non_array_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an array");

            String result = multiSelectFieldMapper.toValue(textNode);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle array with missing id field")
        void should_handle_array_with_missing_id_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option1 = objectMapper.createObjectNode();
            option1.put("value", "Option 1");
            arrayNode.add(option1);

            String result = multiSelectFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle array with null id field")
        void should_handle_array_with_null_id_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option1 = objectMapper.createObjectNode();
            option1.putNull("id");
            option1.put("value", "Option 1");
            arrayNode.add(option1);

            String result = multiSelectFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[]");
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = multiSelectFieldMapper.toStringValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should extract values from single option")
        void should_extract_values_from_single_option() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option = objectMapper.createObjectNode();
            option.put("id", "10000");
            option.put("value", "Option 1");
            arrayNode.add(option);

            String result = multiSelectFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Option 1");
        }

        @Test
        @DisplayName("should extract values from multiple options")
        void should_extract_values_from_multiple_options() {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            ObjectNode option1 = objectMapper.createObjectNode();
            option1.put("id", "10000");
            option1.put("value", "Option 1");
            arrayNode.add(option1);

            ObjectNode option2 = objectMapper.createObjectNode();
            option2.put("id", "10001");
            option2.put("value", "Option 2");
            arrayNode.add(option2);

            ObjectNode option3 = objectMapper.createObjectNode();
            option3.put("id", "10002");
            option3.put("value", "Option 3");
            arrayNode.add(option3);

            String result = multiSelectFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Option 1, Option 2, Option 3");
        }

        @Test
        @DisplayName("should handle empty array")
        void should_handle_empty_array() {
            ArrayNode emptyArray = objectMapper.createArrayNode();

            String result = multiSelectFieldMapper.toStringValue(emptyArray);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle non-array JSON node")
        void should_handle_non_array_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an array");

            String result = multiSelectFieldMapper.toStringValue(textNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle array with missing value field")
        void should_handle_array_with_missing_value_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option1 = objectMapper.createObjectNode();
            option1.put("id", "10000");
            arrayNode.add(option1);

            String result = multiSelectFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle array with null value field")
        void should_handle_array_with_null_value_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode option1 = objectMapper.createObjectNode();
            option1.put("id", "10000");
            option1.putNull("value");
            arrayNode.add(option1);

            String result = multiSelectFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("");
        }
    }
}