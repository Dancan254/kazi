package com.kazi.kazi.search;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class JobSearchService {

    private final JobSearchRepository jobSearchRepository;
    private final JobSourceLinkRepository sourceLinkRepository;

    JobSearchService(JobSearchRepository jobSearchRepository, JobSourceLinkRepository sourceLinkRepository) {
        this.jobSearchRepository = jobSearchRepository;
        this.sourceLinkRepository = sourceLinkRepository;
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

    public JobDetailResult getJob(UUID jobId) {
        Job job = jobSearchRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        var sources = sourceLinkRepository.findByJob_IdOrderByFirstSeenAtAsc(jobId).stream()
            .map(JobSourceResponse::from)
            .toList();
        return JobDetailResult.from(job, sources);
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

    public record JobDetailResult(
        UUID id,
        String title,
        String company,
        String location,
        boolean remote,
        Integer salaryMin,
        Integer salaryMax,
        String currency,
        String description,
        String status,
        EmploymentType employmentType,
        Instant createdAt,
        Instant updatedAt,
        List<JobSourceResponse> sources
    ) {

        private static JobDetailResult from(Job job, List<JobSourceResponse> sources) {
            return new JobDetailResult(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.isRemote(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getDescription(),
                job.getStatus(),
                job.getEmploymentType(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                sources
            );
        }
    }

    public record JobSourceResponse(
        String sourceName,
        String sourceUrl,
        String sourceJobId,
        Instant firstSeenAt,
        Instant lastSeenAt
    ) {

        private static JobSourceResponse from(JobSourceLink source) {
            return new JobSourceResponse(
                source.getSourceName(),
                source.getSourceUrl(),
                source.getSourceJobId(),
                source.getFirstSeenAt(),
                source.getLastSeenAt()
            );
        }
    }
}
