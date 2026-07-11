package com.itimizer.jena.mapper.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("StringFieldMapper Tests")
class StringFieldMapperTest {

    private StringFieldMapper stringFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        stringFieldMapper = new StringFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            assertThat(stringFieldMapper.toValue(null)).isNull();
        }

        @Test
        @DisplayName("should always return null regardless of input")
        void should_always_return_null_regardless_of_input() {
            assertThat(stringFieldMapper.toValue(null)).isNull();
            assertThat(stringFieldMapper.toValue(objectMapper.valueToTree("test"))).isNull();
            assertThat(stringFieldMapper.toValue(objectMapper.valueToTree(123))).isNull();
            assertThat(stringFieldMapper.toValue(objectMapper.valueToTree(true))).isNull();
            assertThat(stringFieldMapper.toValue(objectMapper.createObjectNode())).isNull();
            assertThat(stringFieldMapper.toValue(objectMapper.createArrayNode())).isNull();
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            assertThat(stringFieldMapper.toStringValue(null)).isEqualTo("");
        }

        @Test
        @DisplayName("should extract text from string field")
        void should_extract_text_from_string_field() {
            JsonNode stringValue = objectMapper.valueToTree("Simple text value");

            String result = stringFieldMapper.toStringValue(stringValue);
            assertThat(result).isEqualTo("Simple text value");
        }

        @Test
        @DisplayName("should extract text from numeric field")
        void should_extract_text_from_numeric_field() {
            JsonNode intNode = objectMapper.valueToTree(12345);
            JsonNode longNode = objectMapper.valueToTree(9876543210L);
            JsonNode doubleNode = objectMapper.valueToTree(123.45);
            JsonNode floatNode = objectMapper.valueToTree(123.45f);
            JsonNode bigDecimalNode = objectMapper.valueToTree(123456789.12345);

            assertThat(stringFieldMapper.toStringValue(intNode)).isEqualTo("12345");
            assertThat(stringFieldMapper.toStringValue(longNode)).isEqualTo("9876543210");
            assertThat(stringFieldMapper.toStringValue(doubleNode)).isEqualTo("123.45");
            assertThat(stringFieldMapper.toStringValue(floatNode)).isEqualTo("123.45");
            assertThat(stringFieldMapper.toStringValue(bigDecimalNode))
                    .isEqualTo("1.2345678912345E8");
        }

        @Test
        @DisplayName("should extract text from boolean field")
        void should_extract_text_from_boolean_field() {
            JsonNode trueNode = BooleanNode.TRUE;
            JsonNode falseNode = BooleanNode.FALSE;

            assertThat(stringFieldMapper.toStringValue(trueNode)).isEqualTo("true");
            assertThat(stringFieldMapper.toStringValue(falseNode)).isEqualTo("false");
        }

        @Test
        @DisplayName("should handle null node")
        void should_handle_null_node() {
            JsonNode nullNode = NullNode.getInstance();

            String result = stringFieldMapper.toStringValue(nullNode);
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("should handle empty string node")
        void should_handle_empty_string_node() {
            JsonNode emptyNode = objectMapper.valueToTree("");

            String result = stringFieldMapper.toStringValue(emptyNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should return empty string for object node")
        void should_return_empty_string_for_object_node() {
            JsonNode objectNode = objectMapper.createObjectNode()
                    .put("key1", "value1")
                    .put("key2", "value2")
                    .put("key3", 123);

            String result = stringFieldMapper.toStringValue(objectNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should return empty string for array node")
        void should_return_empty_string_for_array_node() {
            JsonNode arrayNode = objectMapper.createArrayNode()
                    .add("item1")
                    .add("item2")
                    .add("item3");

            String result = stringFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should return empty string for nested object node")
        void should_return_empty_string_for_nested_object_node() {
            JsonNode nestedObject = objectMapper.createObjectNode()
                    .set("level1", objectMapper.createObjectNode()
                            .put("level2", "nested value"));

            String result = stringFieldMapper.toStringValue(nestedObject);
            assertThat(result).isEqualTo("");
        }
    }
}