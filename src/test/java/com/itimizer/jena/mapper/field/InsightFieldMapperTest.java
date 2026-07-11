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

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsightFieldMapper Tests")
class InsightFieldMapperTest {

    private InsightFieldMapper insightFieldMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        insightFieldMapper = new InsightFieldMapper();
    }

    @Nested
    @DisplayName("toValue() method tests")
    class ToValueTests {

        @Test
        @DisplayName("Should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = insightFieldMapper.toValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should extract ID from single insight object in array")
        void should_extract_id_from_single_insight_object_in_array() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (SVR-001)");

            String result = insightFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[SVR-001]");
        }

        @Test
        @DisplayName("should extract IDs from multiple insight objects")
        void should_extract_ids_from_multiple_insight_objects() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (SVR-001)");
            arrayNode.add("Database-01 (DB-001)");
            arrayNode.add("Network Device (NET-123)");

            String result = insightFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[SVR-001, DB-001, NET-123]");
        }

        @Test
        @DisplayName("should handle empty array")
        void should_handle_empty_array() {
            ArrayNode emptyArray = objectMapper.createArrayNode();

            String result = insightFieldMapper.toValue(emptyArray);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle non-array JSON node")
        void should_handle_non_array_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an array");

            String result = insightFieldMapper.toValue(textNode);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle values without parentheses")
        void should_handle_values_without_parentheses() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01");

            String result = insightFieldMapper.toValue(arrayNode);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle values with multiple parentheses")
        void should_handle_values_with_multiple_parentheses() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (Primary) (SVR-001)");

            String result = insightFieldMapper.toValue(arrayNode);
            assertThat(result).isEqualTo("[SVR-001]");
        }
    }

    @Nested
    @DisplayName("toStringValue() method tests")
    class ToStringValueTests {

        @Test
        @DisplayName("should return null when input is null")
        void should_return_null_when_input_is_null() {
            String result = insightFieldMapper.toStringValue(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should extract names from single insight object")
        void should_extract_names_from_single_insight_object() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (SVR-001)");

            String result = insightFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Server-01");
        }

        @Test
        @DisplayName("should extract names from multiple insight objects")
        void should_extract_names_from_multiple_insight_objects() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (SVR-001)");
            arrayNode.add("Database-01 (DB-001)");
            arrayNode.add("Network Device (NET-123)");

            String result = insightFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Server-01, Database-01, Network Device");
        }

        @Test
        @DisplayName("should handle names with special characters")
        void should_handle_names_with_special_characters() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (Production) (SVR-001)");
            arrayNode.add("Database [Primary] (DB-001)");

            String result = insightFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Server-01 (Production), Database [Primary]");
        }

        @Test
        @DisplayName("should handle empty array")
        void should_handle_empty_array() {
            ArrayNode emptyArray = objectMapper.createArrayNode();

            String result = insightFieldMapper.toStringValue(emptyArray);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle non-array JSON node")
        void should_handle_non_array_json_node() {
            JsonNode textNode = objectMapper.valueToTree("Not an array");

            String result = insightFieldMapper.toStringValue(textNode);
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("should handle values without parentheses")
        void should_handle_values_without_parentheses() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01");

            String result = insightFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Server-01");
        }

        @Test
        @DisplayName("should handle values with multiple parentheses")
        void should_handle_values_with_multiple_parentheses() {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            arrayNode.add("Server-01 (Primary) (Backup) (SVR-001)");

            String result = insightFieldMapper.toStringValue(arrayNode);
            assertThat(result).isEqualTo("Server-01 (Primary) (Backup)");
        }
    }
}