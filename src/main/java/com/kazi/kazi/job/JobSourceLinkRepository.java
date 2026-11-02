package com.kazi.kazi.job;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface JobSourceLinkRepository extends ListCrudRepository<JobSourceLink, UUID> {

    Optional<JobSourceLink> findByJob_IdAndSourceName(UUID jobId, String sourceName);
}
