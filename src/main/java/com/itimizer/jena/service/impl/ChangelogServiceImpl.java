package com.itimizer.jena.service.impl;

import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.dto.ChangelogDto;
import com.itimizer.jena.entity.Changelog;
import com.itimizer.jena.exception.ObjectNotFoundException;
import com.itimizer.jena.mapper.ChangelogMapper;
import com.itimizer.jena.repository.ChangelogRepository;
import com.itimizer.jena.service.ChangelogService;
import com.itimizer.jena.service.JiraFilterService;
import com.itimizer.jena.service.RuleService;
import com.itimizer.jena.transactionalmanager.TransactionRunner;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link ChangelogService} backed by JPA. Creation is idempotent: a unique constraint on the
 * (event, item, index, issue, rule, filter) tuple makes a concurrent or repeated poll collapse to
 * the existing row instead of inserting a duplicate notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangelogServiceImpl implements ChangelogService {

    private final ChangelogRepository changelogRepository;
    private final ChangelogMapper changelogMapper;
    private final TransactionRunner transactionRunner;
    private final RuleService ruleService;
    private final JiraFilterService jiraFilterService;

    @Override
    public ChangelogDto create(@NonNull ChangelogCreateDto dto) {
        if (ruleService.get(dto.rule()) == null) {
            throw new ObjectNotFoundException("Notification rule with id "
                    + dto.rule() + " not found.");
        }
        if (jiraFilterService.fetch(dto.filter()) == null) {
            throw new ObjectNotFoundException("Jira filter with id "
                    + dto.filter() + " not found.");
        }
        var changelog = changelogMapper.fromDto(dto);

        try {
            return changelogMapper.toDto(transactionRunner
                    .doInTransaction(() -> changelogRepository.save(changelog)));
        } catch (DataIntegrityViolationException e) {
            // Same item already captured (concurrent poll or replay): return the existing row.
            log.debug("Changelog already exists for rule {}, filter {}, issue {}, event {}, "
                            + "item {}/{} - skipping duplicate",
                    dto.rule(), dto.filter(), dto.issueKey(), dto.event(),
                    dto.item(), dto.itemIndex());
            return changelogRepository
                    .findByEventAndItemAndItemIndexAndIssueKeyAndRuleIdAndFilterId(
                            dto.event(), dto.item(), dto.itemIndex(), dto.issueKey(),
                            dto.rule(), dto.filter())
                    .map(changelogMapper::toDto)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public ChangelogDto update(@NonNull ChangelogDto dto) {
        if (ruleService.get(dto.rule()) == null) {
            throw new ObjectNotFoundException("Notification rule with id "
                    + dto.rule() + " not found.");
        }
        return changelogMapper.toDto(transactionRunner.doInTransaction(() -> {
            var changelog = changelogRepository.findById(dto.id()).orElse(null);

            if (changelog == null) {
                throw new ObjectNotFoundException("Changelog with id "
                        + dto.id() + " not found.");
            }
            changelogMapper.updateChangelogFromDto(dto, changelog);
            return changelogRepository.save(changelog);
        })
        );
    }

    @Override
    public Changelog update(@NonNull Changelog changelog) {
        if (ruleService.get(changelog.getRule().getId()) == null) {
            throw new ObjectNotFoundException("Notification rule with id "
                    + changelog.getRule().getId() + " not found.");
        }
        return transactionRunner.doInTransaction(() -> changelogRepository.save(changelog));
    }

    @Override
    public List<Changelog> findNewChangelog() {
        return changelogRepository.findByStatusIsNull();
    }
}
