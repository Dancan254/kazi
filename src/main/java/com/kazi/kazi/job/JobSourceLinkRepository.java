package com.kazi.kazi.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface JobSourceLinkRepository extends ListCrudRepository<JobSourceLink, UUID> {

    Optional<JobSourceLink> findBySourceNameAndSourceJobId(String sourceName, String sourceJobId);

    List<JobSourceLink> findByJob_IdOrderByFirstSeenAtAsc(UUID jobId);

    List<JobSourceLink> findBySourceNameAndActiveTrue(String sourceName);

    boolean existsByJob_IdAndActiveTrue(UUID jobId);
}
