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
@DisplayName("MultiUserFieldMapper Tests")
class MultiUserFieldMapperTest {

    private MultiUserFieldMapper multiUserFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        multiUserFieldMapper = new MultiUserFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = multiUserFieldMapper.toValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should extract user keys from single user")
        void should_extract_user_keys_from_single_user() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user = objectMapper.createObjectNode();
            user.put("key", "john.doe");
            user.put("displayName", "John Doe");
            arrayNode.add(user);

            String result = multiUserFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[john.doe]");
        }

        @Test
        @DisplayName("should extract user keys from multiple users")
        void should_extract_user_keys_from_multiple_users() {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            user1.put("displayName", "John Doe");
            arrayNode.add(user1);

            ObjectNode user2 = objectMapper.createObjectNode();
            user2.put("key", "jane.smith");
            user2.put("displayName", "Jane Smith");
            arrayNode.add(user2);

            ObjectNode user3 = objectMapper.createObjectNode();
            user3.put("key", "bob.johnson");
            user3.put("displayName", "Bob Johnson");
            arrayNode.add(user3);

            String result = multiUserFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[john.doe, jane.smith, bob.johnson]");
        }

        @Test
        @DisplayName("should handle empty array")
        void should_handle_empty_array() {
            ArrayNode emptyArray = objectMapper.createArrayNode();

            String result = multiUserFieldMapper.toValue(emptyArray);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle non-array JSON node")
        void should_handle_non_array_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an array");

            String result = multiUserFieldMapper.toValue(textNode);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle array with missing key field")
        void should_handle_array_with_missing_key_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("displayName", "John Doe");
            arrayNode.add(user1);

            String result = multiUserFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle array with null key field")
        void should_handle_array_with_null_key_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user1 = objectMapper.createObjectNode();
            user1.putNull("key");
            user1.put("displayName", "John Doe");
            arrayNode.add(user1);

            String result = multiUserFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle array with mixed valid and invalid users")
        void should_handle_array_with_mixed_valid_and_invalid_users() {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            user1.put("displayName", "John Doe");
            arrayNode.add(user1);

            ObjectNode user2 = objectMapper.createObjectNode();
            user2.put("displayName", "Jane Smith");
            arrayNode.add(user2);

            ObjectNode user3 = objectMapper.createObjectNode();
            user3.put("key", "bob.johnson");
            user3.put("displayName", "Bob Johnson");
            arrayNode.add(user3);

            String result = multiUserFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[john.doe, bob.johnson]");
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = multiUserFieldMapper.toStringValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should extract display names from single user")
        void should_extract_display_names_from_single_user() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user = objectMapper.createObjectNode();
            user.put("key", "john.doe");
            user.put("displayName", "John Doe");
            arrayNode.add(user);

            String result = multiUserFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should extract display names from multiple users")
        void should_extract_display_names_from_multiple_users() {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            user1.put("displayName", "John Doe");
            arrayNode.add(user1);

            ObjectNode user2 = objectMapper.createObjectNode();
            user2.put("key", "jane.smith");
            user2.put("displayName", "Jane Smith");
            arrayNode.add(user2);

            ObjectNode user3 = objectMapper.createObjectNode();
            user3.put("key", "bob.johnson");
            user3.put("displayName", "Bob Johnson");
            arrayNode.add(user3);

            String result = multiUserFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("John Doe, Jane Smith, Bob Johnson");
        }

        @Test
        @DisplayName("should handle empty array")
        void should_handle_empty_array() {
            ArrayNode emptyArray = objectMapper.createArrayNode();

            String result = multiUserFieldMapper.toStringValue(emptyArray);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle non-array JSON node")
        void should_handle_non_array_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an array");

            String result = multiUserFieldMapper.toStringValue(textNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle array with missing displayName field")
        void should_handle_array_with_missing_displayName_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            arrayNode.add(user1);

            String result = multiUserFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle array with null displayName field")
        void should_handle_array_with_null_displayName_field() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            user1.putNull("displayName");
            arrayNode.add(user1);

            String result = multiUserFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle array with empty displayName")
        void should_handle_array_with_empty_displayName() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            user1.put("displayName", "");
            arrayNode.add(user1);

            String result = multiUserFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle array with mixed valid and invalid users")
        void should_handle_array_with_mixed_valid_and_invalid_users() {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            ObjectNode user1 = objectMapper.createObjectNode();
            user1.put("key", "john.doe");
            user1.put("displayName", "John Doe");
            arrayNode.add(user1);

            ObjectNode user2 = objectMapper.createObjectNode();
            user2.put("key", "jane.smith");
            arrayNode.add(user2);

            ObjectNode user3 = objectMapper.createObjectNode();
            user3.put("key", "bob.johnson");
            user3.put("displayName", "Bob Johnson");
            arrayNode.add(user3);

            String result = multiUserFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("John Doe, Bob Johnson");
        }
    }
}