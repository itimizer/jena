package com.itimizer.jena.mapper;

import com.itimizer.jena.dto.RuleCreateDto;
import com.itimizer.jena.dto.RuleDto;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.Rule;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleMapper Tests")
class RuleMapperTest {

    private RuleMapper ruleMapper;

    @BeforeEach
    void setUp() {
        ruleMapper = Mappers.getMapper(RuleMapper.class);
    }

    @Nested
    @DisplayName("toDto() method tests")
    class ToDtoTests {

        @Test
        @DisplayName("should convert Rule entity to RuleDto")
        void should_convert_rule_entity_to_rule_dto() {
            Template template = new Template();
            template.setId(100L);

            Rule rule = new Rule();
            rule.setId(1L);
            rule.setTemplate(template);
            rule.setEnabled(true);
            rule.setEvent(Event.ISSUE_CREATED);
            rule.setField("status");
            rule.setFrom("1");
            rule.setFromStr("To Do");
            rule.setTo("3");
            rule.setToStr("In Progress");
            rule.setHasChanged(true);
            rule.setCreatedBy("admin");
            rule.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
            rule.setLastModifiedBy("admin");
            rule.setUpdatedAt(Instant.parse("2024-01-01T10:00:00Z"));

            RuleDto dto = ruleMapper.toDto(rule);
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.template()).isEqualTo(100L);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.event()).isEqualTo(Event.ISSUE_CREATED);
            assertThat(dto.field()).isEqualTo("status");
            assertThat(dto.from()).isEqualTo("1");
            assertThat(dto.fromStr()).isEqualTo("To Do");
            assertThat(dto.to()).isEqualTo("3");
            assertThat(dto.toStr()).isEqualTo("In Progress");
            assertThat(dto.hasChanged()).isTrue();
        }

        @Test
        @DisplayName("should handle null optional fields")
        void should_handle_null_optional_fields() {
            Rule rule = new Rule();
            rule.setId(1L);
            rule.setEnabled(true);
            rule.setEvent(Event.ISSUE_CREATED);
            rule.setField(null);
            rule.setFrom(null);
            rule.setFromStr(null);
            rule.setTo(null);
            rule.setToStr(null);
            rule.setHasChanged(false);

            RuleDto dto = ruleMapper.toDto(rule);
            assertThat(dto).isNotNull();
            assertThat(dto.field()).isNull();
            assertThat(dto.from()).isNull();
            assertThat(dto.fromStr()).isNull();
            assertThat(dto.to()).isNull();
            assertThat(dto.toStr()).isNull();
        }
    }

    @Nested
    @DisplayName("fromDto() method tests")
    class ToEntityTests {

        @Test
        @DisplayName("should convert RuleCreateDto to Rule entity")
        void should_convert_rule_create_dto_to_rule_entity() {
            RuleCreateDto createDto = new RuleCreateDto(
                    100L,
                    true,
                    Event.ISSUE_CREATED,
                    "priority",
                    "3",
                    "Medium",
                    "2",
                    "High",
                    true
            );

            Rule rule = ruleMapper.fromDto(createDto);
            assertThat(rule).isNotNull();
            assertThat(rule.getTemplate()).isNotNull();
            assertThat(rule.getTemplate().getId()).isEqualTo(100L);
            assertThat(rule.isEnabled()).isTrue();
            assertThat(rule.getEvent()).isEqualTo(Event.ISSUE_CREATED);
            assertThat(rule.getField()).isEqualTo("priority");
            assertThat(rule.getFrom()).isEqualTo("3");
            assertThat(rule.getFromStr()).isEqualTo("Medium");
            assertThat(rule.getTo()).isEqualTo("2");
            assertThat(rule.getToStr()).isEqualTo("High");
            assertThat(rule.isHasChanged()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateRuleFromDto() method tests")
    class UpdateRuleFromDtoTests {

        private Rule existingRule;

        @BeforeEach
        void setUp() {
            Template template = new Template();
            template.setId(50L);

            existingRule = new Rule();
            existingRule.setId(1L);
            existingRule.setTemplate(template);
            existingRule.setEnabled(true);
            existingRule.setEvent(Event.ISSUE_CREATED);
            existingRule.setField("priority");
            existingRule.setFrom("5");
            existingRule.setFromStr("Low");
            existingRule.setTo("3");
            existingRule.setToStr("Medium");
            existingRule.setHasChanged(false);
        }

        @Test
        @DisplayName("should update Rule entity from RuleDto")
        void should_update_rule_entity_from_rule_dto() {
            RuleDto updateDto = new RuleDto(
                    1L,
                    200L,
                    false,
                    Event.ISSUE_UPDATED,
                    "status",
                    "1",
                    "To Do",
                    "3",
                    "In Progress",
                    true
            );

            ruleMapper.updateRuleFromDto(updateDto, existingRule);
            assertThat(existingRule.getId()).isEqualTo(1L);
            assertThat(existingRule.getTemplate().getId()).isEqualTo(50L);
            assertThat(existingRule.isEnabled()).isFalse();
            assertThat(existingRule.getEvent()).isEqualTo(Event.ISSUE_UPDATED);
            assertThat(existingRule.getField()).isEqualTo("status");
            assertThat(existingRule.getFrom()).isEqualTo("1");
            assertThat(existingRule.getFromStr()).isEqualTo("To Do");
            assertThat(existingRule.getTo()).isEqualTo("3");
            assertThat(existingRule.getToStr()).isEqualTo("In Progress");
            assertThat(existingRule.isHasChanged()).isTrue();
        }
    }
}