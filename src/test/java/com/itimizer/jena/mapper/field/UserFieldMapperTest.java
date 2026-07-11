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
@DisplayName("UserFieldMapper Tests")
class UserFieldMapperTest {

    private UserFieldMapper userFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userFieldMapper = new UserFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            assertThat(userFieldMapper.toValue(null)).isNull();
        }

        @Test
        @DisplayName("should extract key from user object")
        void should_extract_key_from_user_object() {
            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.put("key", "john.doe");
            userNode.put("displayName", "John Doe");

            String result = userFieldMapper.toValue(userNode);
            assertThat(result).isEqualTo("john.doe");
        }

        @Test
        @DisplayName("should handle missing key field")
        void should_handle_missing_key_field() {
            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.put("displayName", "John Doe");

            String result = userFieldMapper.toValue(userNode);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle null key field")
        void should_handle_null_key_field() {
            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.putNull("key");
            userNode.put("displayName", "John Doe");

            String result = userFieldMapper.toValue(userNode);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle empty key field")
        void should_handle_empty_key_field() {
            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.put("key", "");
            userNode.put("displayName", "John Doe");

            String result = userFieldMapper.toValue(userNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle non-object JSON node")
        void should_handle_non_object_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an object");

            String result = userFieldMapper.toValue(textNode);
            assertThat(result).isNull();
        }

        @Nested
        @DisplayName("toStringValue() method tests")
        class ToStringValueTests {

            @Test
            @DisplayName("should return empty string when input is null")
            void should_return_null_when_input_is_null() {
                assertThat(userFieldMapper.toStringValue(null)).isEqualTo("");
            }

            @Test
            @DisplayName("should extract displayName from user object")
            void should_extract_displayName_from_user_object() {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("key", "john.doe");
                userNode.put("displayName", "John Doe");

                String result = userFieldMapper.toStringValue(userNode);
                assertThat(result).isEqualTo("John Doe");
            }

            @Test
            @DisplayName("should handle missing displayName field")
            void should_handle_missing_displayName_field() {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("key", "john.doe");

                String result = userFieldMapper.toStringValue(userNode);
                assertThat(result).isEqualTo("");
            }

            @Test
            @DisplayName("should handle null displayName field")
            void should_handle_null_displayName_field() {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("key", "john.doe");
                userNode.putNull("displayName");

                String result = userFieldMapper.toStringValue(userNode);
                assertThat(result).isEqualTo("");
            }

            @Test
            @DisplayName("should handle empty displayName field")
            void should_handle_empty_displayName_field() {
                ObjectNode userNode = objectMapper.createObjectNode();
                userNode.put("key", "john.doe");
                userNode.put("displayName", "");

                String result = userFieldMapper.toStringValue(userNode);
                assertThat(result).isEqualTo("");
            }

            @Test
            @DisplayName("should handle non-object JSON node")
            void shouldHandleNonObjectJsonNode() {
                JsonNode textNode = objectMapper.valueToTree("Not an object");

                String result = userFieldMapper.toStringValue(textNode);
                assertThat(result).isEqualTo("");
            }
        }
    }
}