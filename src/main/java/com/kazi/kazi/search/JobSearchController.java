package com.kazi.kazi.search;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.search.JobSearchService.JobDetailResult;
import com.kazi.kazi.search.JobSearchService.JobSearchResult;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class JobSearchController {

    private final JobSearchService jobSearchService;
    private final SemanticSearchService semanticSearchService;

    JobSearchController(JobSearchService jobSearchService, SemanticSearchService semanticSearchService) {
        this.jobSearchService = jobSearchService;
        this.semanticSearchService = semanticSearchService;
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

    @GetMapping("/api/jobs/search/semantic")
    JobSearchResult searchSemantic(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        return semanticSearchService.search(q, limit);
    }

    @GetMapping("/api/jobs/{jobId}")
    JobDetailResult getJob(@PathVariable UUID jobId) {
        return jobSearchService.getJob(jobId);
    }

    @ExceptionHandler(SemanticSearchUnavailableException.class)
    ProblemDetail semanticSearchUnavailable(SemanticSearchUnavailableException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Semantic search unavailable");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidSearch(IllegalArgumentException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid search");
        return problem;
    }

    @ExceptionHandler(JobNotFoundException.class)
    ProblemDetail jobNotFound(JobNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Job not found");
        return problem;
    }
}
