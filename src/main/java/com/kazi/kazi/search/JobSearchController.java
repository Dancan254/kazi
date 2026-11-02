package com.kazi.kazi.search;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.search.JobSearchService.JobSearchResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class JobSearchController {

    private final JobSearchService jobSearchService;

    JobSearchController(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
    }

    @GetMapping("/api/jobs")
    JobSearchResult search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) EmploymentType employmentType,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return jobSearchService.search(q, location, employmentType, pageable);
    }
}
