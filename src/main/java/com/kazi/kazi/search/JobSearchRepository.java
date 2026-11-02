package com.kazi.kazi.search;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.job.Job;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface JobSearchRepository extends ListCrudRepository<Job, UUID> {

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'active'
          AND (LOWER(j.title) LIKE LOWER(:keywordPattern)
            OR LOWER(j.company) LIKE LOWER(:keywordPattern)
            OR LOWER(j.description) LIKE LOWER(:keywordPattern))
          AND COALESCE(LOWER(j.location), '') LIKE LOWER(:locationPattern)
          AND (:employmentType IS NULL OR j.employmentType = :employmentType)
        """)
    Page<Job> search(
        @Param("keywordPattern") String keywordPattern,
        @Param("locationPattern") String locationPattern,
        @Param("employmentType") EmploymentType employmentType,
        Pageable pageable
    );
}
