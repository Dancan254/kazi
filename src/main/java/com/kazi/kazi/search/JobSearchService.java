package com.kazi.kazi.search;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.job.Job;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class JobSearchService {

    private final JobSearchRepository jobSearchRepository;

    JobSearchService(JobSearchRepository jobSearchRepository) {
        this.jobSearchRepository = jobSearchRepository;
    }

    public JobSearchResult search(
        String query,
        String location,
        EmploymentType employmentType,
        Pageable pageable
    ) {
        Page<Job> jobs = jobSearchRepository.search(pattern(query), pattern(location), employmentType, pageable);
        return new JobSearchResult(
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

    public record JobSearchResult(
        List<JobResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    public record JobResponse(
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

        private static JobResponse from(Job job) {
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
