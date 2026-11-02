package com.kazi.kazi.ingestion.run;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface SourceRunRepository extends ListCrudRepository<SourceRun, UUID> {

    Optional<SourceRun> findFirstBySourceNameAndStatusOrderByFinishedAtDesc(
        String sourceName,
        SourceRunStatus status
    );
}
