package com.itimizer.jena.service;

import com.itimizer.jena.dto.ChangelogCreateDto;
import com.itimizer.jena.dto.ChangelogDto;
import com.itimizer.jena.entity.Changelog;
import lombok.NonNull;

import java.util.List;

/**
 * Persists and queries {@link Changelog} rows — the matched-changelog records that bridge polling
 * (which creates them) and dispatch (which consumes them). INCREMENTAL mode only.
 */
public interface ChangelogService {

    ChangelogDto create(@NonNull ChangelogCreateDto dto);

    ChangelogDto update(@NonNull ChangelogDto dto);

    Changelog update(@NonNull Changelog changelog);

    /** Rows still awaiting dispatch (status {@code NEW}); the dispatch job's work queue. */
    List<Changelog> findNewChangelog();
}
