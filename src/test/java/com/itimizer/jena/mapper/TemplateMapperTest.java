package com.itimizer.jena.mapper;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import com.itimizer.jena.dto.ExpressTemplateDto;
import com.itimizer.jena.dto.JiraEmailTemplateDto;
import com.itimizer.jena.dto.TelegramTemplateDto;
import com.itimizer.jena.dto.TemplateContentDto;
import com.itimizer.jena.dto.TemplateCreateDto;
import com.itimizer.jena.dto.TemplateDto;
import com.itimizer.jena.entity.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateMapper Tests")
class TemplateMapperTest {

    private TemplateMapper templateMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        templateMapper = Mappers.getMapper(TemplateMapper.class);
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("toDto() method tests")
    class ToDtoTests {

        @Test
        @DisplayName("should convert Template entity to TemplateDto")
        void should_convert_template_entity_to_template_dto() throws JacksonException {
            TemplateContentDto contentDto = new TemplateContentDto(
                    new TelegramTemplateDto("Telegram message"),
                    new JiraEmailTemplateDto("Email message"),
                    new ExpressTemplateDto("Express message")
            );
            String contentJson = objectMapper.writeValueAsString(contentDto);

            Template template = new Template();
            template.setId(1L);
            template.setName("Test Template");
            template.setContent(contentJson);
            template.setCreatedBy("admin");
            template.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
            template.setLastModifiedBy("admin");
            template.setUpdatedAt(Instant.parse("2024-01-01T10:00:00Z"));

            TemplateDto dto = templateMapper.toDto(template);
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Test Template");
            assertThat(dto.content()).isNotNull();
            assertThat(dto.content().telegram()).isNotNull();
            assertThat(dto.content().telegram().message()).isEqualTo("Telegram message");
            assertThat(dto.content().jiraEmail()).isNotNull();
            assertThat(dto.content().jiraEmail().message()).isEqualTo("Email message");
        }

        @Test
        @DisplayName("should handle null content")
        void should_handle_null_content() {
            Template template = new Template();
            template.setId(1L);
            template.setName("Test Template");
            template.setContent(null);

            assertThatThrownBy(() -> templateMapper.toDto(template))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should handle invalid JSON content")
        void should_handle_invalid_json_content() {
            Template template = new Template();
            template.setId(1L);
            template.setName("Test Template");
            template.setContent("invalid json content");

            assertThatThrownBy(() -> templateMapper.toDto(template))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(StreamReadException.class);
        }

        @Test
        @DisplayName("should handle empty content")
        void should_handle_empty_content() {
            Template template = new Template();
            template.setId(1L);
            template.setName("Test Template");
            template.setContent("");

            assertThatThrownBy(() -> templateMapper.toDto(template))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(MismatchedInputException.class);
        }
    }

    @Nested
    @DisplayName("fromDto() method tests")
    class FromDtoTests {

        @Test
        @DisplayName("should convert dto to entity")
        void should_convert_dto_to_entity() throws JacksonException {
            TemplateContentDto contentDto = new TemplateContentDto(
                    new TelegramTemplateDto("Telegram message"),
                    new JiraEmailTemplateDto("Email message"),
                    new ExpressTemplateDto("Express message")
            );
            TemplateCreateDto createDto = new TemplateCreateDto(
                    "New Template",
                    contentDto
            );

            Template template = templateMapper.fromDto(createDto);
            assertThat(template).isNotNull();
            assertThat(template.getName()).isEqualTo("New Template");
            assertThat(template.getContent()).isNotNull();

            TemplateContentDto deserializedContent = objectMapper.readValue(
                    template.getContent(),
                    TemplateContentDto.class
            );
            assertThat(deserializedContent.telegram().message()).isEqualTo("Telegram message");
            assertThat(deserializedContent.jiraEmail().message()).isEqualTo("Email message");
        }

        @Test
        @DisplayName("should handle create DTO with null content")
        void should_handle_create_dto_with_null_content() {
            TemplateCreateDto createDto = new TemplateCreateDto(
                    "Template with null content",
                    null
            );

            Template template = templateMapper.fromDto(createDto);
            assertThat(template.getContent()).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("updateTemplateFromDto() method tests")
    class UpdateTemplateFromDtoTests {

        private Template existingTemplate;

        @BeforeEach
        void setUp() throws JacksonException {
            existingTemplate = new Template();
            existingTemplate.setId(1L);
            existingTemplate.setName("Original Template");
            TemplateContentDto originalContent = new TemplateContentDto(
                    new TelegramTemplateDto("Original telegram"),
                    new JiraEmailTemplateDto("Original email"),
                    new ExpressTemplateDto("Original express")
            );
            existingTemplate.setContent(objectMapper.writeValueAsString(originalContent));
            existingTemplate.setCreatedBy("admin");
            existingTemplate.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        }

        @Test
        @DisplayName("should update Template entity from TemplateDto")
        void should_update_template_entity_from_template_dto() throws JacksonException {
            TemplateContentDto newContent = new TemplateContentDto(
                    new TelegramTemplateDto("Updated telegram"),
                    new JiraEmailTemplateDto("Updated email"),
                    new ExpressTemplateDto("Updated express")
            );

            TemplateDto updateDto = new TemplateDto(
                    1L,
                    "Updated Template",
                    newContent
            );

            templateMapper.updateTemplateFromDto(updateDto, existingTemplate);
            assertThat(existingTemplate.getId()).isEqualTo(1L);
            assertThat(existingTemplate.getName()).isEqualTo("Updated Template");

            TemplateContentDto deserializedContent = objectMapper.readValue(
                    existingTemplate.getContent(),
                    TemplateContentDto.class
            );
            assertThat(deserializedContent.telegram().message()).isEqualTo("Updated telegram");
            assertThat(deserializedContent.jiraEmail().message()).isEqualTo("Updated email");

            assertThat(existingTemplate.getCreatedBy()).isEqualTo("admin");
            assertThat(existingTemplate.getCreatedAt())
                    .isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
        }
    }

    @Nested
    @DisplayName("convertContentToEntity() method tests")
    class ConvertContentToEntityTests {

        @Test
        @DisplayName("should convert TemplateContentDto to JSON string")
        void should_convert_template_content_dto_to_json_string() throws JacksonException {
            TemplateContentDto contentDto = new TemplateContentDto(
                    new TelegramTemplateDto("Telegram message"),
                    new JiraEmailTemplateDto("Email message"),
                    new ExpressTemplateDto("Express message")
            );

            String json = templateMapper.convertContentToEntity(contentDto);
            assertThat(json).isNotNull();
            assertThat(json).contains("{\"telegram\":{\"message\":\"Telegram message\"},"
                    + "\"jira-email\":{\"message\":\"Email message\"},"
                    + "\"express\":{\"message\":\"Express message\"}}");
        }

        @Test
        @DisplayName("should handle null content")
        void should_handle_null_content() throws JacksonException {
            String json = templateMapper.convertContentToEntity(null);
            assertThat(json).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("convertContentToDto() method tests")
    class ConvertContentToDtoTests {

        @Test
        @DisplayName("should convert JSON string to TemplateContentDto")
        void should_convert_json_string_to_template_content_dto() throws JacksonException {
            String json = "{\"telegram\":{\"message\":\"Telegram message\"},"
                    + "\"jira-email\":{\"message\":\"Email message\"}}";

            TemplateContentDto contentDto = templateMapper.convertContentToDto(json);
            assertThat(contentDto).isNotNull();
            assertThat(contentDto.telegram()).isNotNull();
            assertThat(contentDto.telegram().message()).isEqualTo("Telegram message");
            assertThat(contentDto.jiraEmail()).isNotNull();
            assertThat(contentDto.jiraEmail().message()).isEqualTo("Email message");
        }

        @Test
        @DisplayName("should throw exception for invalid JSON")
        void should_throw_exception_for_invalid_json() {
            String invalidJson = "invalid json string";

            assertThatThrownBy(() -> templateMapper.convertContentToDto(invalidJson))
                    .isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("should handle empty JSON")
        void should_handle_empty_json() {
            assertThatThrownBy(() -> templateMapper.convertContentToDto(""))
                    .isInstanceOf(JacksonException.class);
        }
    }
}