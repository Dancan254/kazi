package com.kazi.kazi.search;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.job.Job;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class JobSearchController {

    private final JobSearchRepository jobSearchRepository;

    JobSearchController(JobSearchRepository jobSearchRepository) {
        this.jobSearchRepository = jobSearchRepository;
    }

    @GetMapping("/api/jobs")
    JobSearchResponse search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) EmploymentType employmentType,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Job> jobs = jobSearchRepository.search(pattern(q), pattern(location), employmentType, pageable);
        return new JobSearchResponse(
            jobs.getContent().stream().map(JobResponse::from).toList(),
            jobs.getNumber(),
            jobs.getSize(),
            jobs.getTotalElements(),
            jobs.getTotalPages()
        );
    }

    private String pattern(String value) {
        return value == null || value.isBlank() ? "%" : "%" + value.trim() + "%";
    }

    record JobSearchResponse(
        java.util.List<JobResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    record JobResponse(
        UUID id,
        String title,
        String company,
        String location,
        boolean remote,
        Integer salaryMin,
        Integer salaryMax,
        String currency,
        String description,
        EmploymentType employmentType,
        Instant createdAt,
        Instant updatedAt
    ) {

        static JobResponse from(Job job) {
            return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.isRemote(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getDescription(),
                job.getEmploymentType(),
                job.getCreatedAt(),
                job.getUpdatedAt()
            );
        }
    }
}
