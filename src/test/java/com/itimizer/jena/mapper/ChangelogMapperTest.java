package com.itimizer.jena.mapper;

import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.dto.ChangelogDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.entity.Event;
import com.itimizer.jena.entity.Rule;
import com.itimizer.jena.entity.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangelogMapper Tests")
class ChangelogMapperTest {

    private ChangelogMapper changelogMapper;

    @BeforeEach
    void setUp() {
        changelogMapper = Mappers.getMapper(ChangelogMapper.class);
    }

    @Nested
    @DisplayName("toDto() method tests")
    class ToDtoTests {

        @Test
        @DisplayName("should convert Changelog entity to ChangelogDto")
        void should_convert_changelog_entity_to_changelog_dto() {
            Rule rule = new Rule();
            rule.setId(100L);

            Map<String, Object> context = new HashMap<>();
            context.put("summary", "test issue");
            context.put("customfield_10000", 123);

            Changelog changelog = new Changelog();
            changelog.setId(1L);
            changelog.setEvent(Event.ISSUE_CREATED);
            changelog.setItem(12345L);
            changelog.setItemIndex(0);
            changelog.setIssueKey("PROJ-123");
            changelog.setRule(rule);
            changelog.setContext(context);
            changelog.setStatus(Status.SUCCESS);
            changelog.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            changelog.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

            ChangelogDto dto = changelogMapper.toDto(changelog);
            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.event()).isEqualTo(Event.ISSUE_CREATED);
            assertThat(dto.item()).isEqualTo(12345L);
            assertThat(dto.itemIndex()).isEqualTo(0);
            assertThat(dto.issueKey()).isEqualTo("PROJ-123");
            assertThat(dto.rule()).isEqualTo(100L);
            assertThat(dto.context()).containsEntry("summary", "test issue");
            assertThat(dto.context()).containsEntry("customfield_10000", 123);
            assertThat(dto.status()).isEqualTo(Status.SUCCESS);
        }
    }

    @Nested
    @DisplayName("fromDto() method tests")
    class FromDtoTests {

        @Test
        @DisplayName("should convert ChangelogCreateDto to Changelog entity")
        void should_convert_changelog_create_dto_to_changelog_entity() {
            Map<String, Object> context = new HashMap<>();
            context.put("priority", "High");
            context.put("assignee", "john.doe");

            ChangelogCreateDto createDto = new ChangelogCreateDto(
                    Event.ISSUE_CREATED,
                    12345L,
                    0,
                    "PROJ-123",
                    100L,
                    200L,
                    context
            );

            Changelog changelog = changelogMapper.fromDto(createDto);
            assertThat(changelog).isNotNull();
            assertThat(changelog.getEvent()).isEqualTo(Event.ISSUE_CREATED);
            assertThat(changelog.getItem()).isEqualTo(12345L);
            assertThat(changelog.getItemIndex()).isEqualTo(0);
            assertThat(changelog.getIssueKey()).isEqualTo("PROJ-123");
            assertThat(changelog.getContext()).containsEntry("priority", "High");
            assertThat(changelog.getContext()).containsEntry("assignee", "john.doe");
            assertThat(changelog.getRule()).isNotNull();
            assertThat(changelog.getRule().getId()).isEqualTo(100L);
            assertThat(changelog.getFilter()).isNotNull();
            assertThat(changelog.getFilter().getId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("updateChangelogFromDto() method tests")
    class UpdateChangelogFromDtoTests {

        private Changelog existingChangelog;

        @BeforeEach
        void setUp() {
            Rule rule = new Rule();
            rule.setId(100L);

            Map<String, Object> context = new HashMap<>();
            context.put("summary", "test issue");

            existingChangelog = new Changelog();
            existingChangelog.setId(1L);
            existingChangelog.setEvent(Event.ISSUE_CREATED);
            existingChangelog.setItem(12345L);
            existingChangelog.setItemIndex(0);
            existingChangelog.setIssueKey("PROJ-123");
            existingChangelog.setRule(rule);
            existingChangelog.setContext(context);
            existingChangelog.setStatus(Status.ERROR);
        }

        @Test
        @DisplayName("should update Changelog entity from DTO")
        void should_update_changelog_entity_from_dto() {
            Map<String, Object> newContext = new HashMap<>();
            newContext.put("priority", "Critical");
            newContext.put("customfield_10000", 123);

            ChangelogDto updateDto = new ChangelogDto(
                    1L,
                    Event.ISSUE_UPDATED,
                    67890L,
                    1,
                    "PROJ-456",
                    200L,
                    newContext,
                    Status.SUCCESS
            );

            changelogMapper.updateChangelogFromDto(updateDto, existingChangelog);
            assertThat(existingChangelog.getId()).isEqualTo(1L);
            assertThat(existingChangelog.getEvent()).isEqualTo(Event.ISSUE_UPDATED);
            assertThat(existingChangelog.getItem()).isEqualTo(67890L);
            assertThat(existingChangelog.getItemIndex()).isEqualTo(1);
            assertThat(existingChangelog.getIssueKey()).isEqualTo("PROJ-456");
            assertThat(existingChangelog.getRule().getId()).isEqualTo(200L);
            assertThat(existingChangelog.getContext()).containsEntry("priority", "Critical");
            assertThat(existingChangelog.getContext()).containsEntry("customfield_10000", 123);
            assertThat(existingChangelog.getStatus()).isEqualTo(Status.SUCCESS);
        }
    }
}