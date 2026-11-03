package com.kazi.kazi.mcp;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.search.JobSearchService;
import com.kazi.kazi.search.JobSearchService.JobDetailResult;
import com.kazi.kazi.search.JobSearchService.JobSearchResult;
import com.kazi.kazi.search.SemanticSearchService;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JobMcpTools {

    private final JobSearchService jobSearchService;
    private final SemanticSearchService semanticSearchService;

    JobMcpTools(JobSearchService jobSearchService, SemanticSearchService semanticSearchService) {
        this.jobSearchService = jobSearchService;
        this.semanticSearchService = semanticSearchService;
    }

    @McpTool(
        name = "semantic_search_jobs",
        description = "Find active jobs by meaning rather than exact keywords using local pgvector embeddings. "
            + "Returns at most 20 matches, closest first. Requires semantic search to be enabled.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public JobSearchResult semanticSearchJobs(
        @McpToolParam(description = "Role, skills, or domain to find, e.g. 'distributed Java backend'", required = true)
        String query,
        @McpToolParam(description = "Maximum results to return, from 1 to 20; defaults to 20", required = false)
        Integer limit
    ) {
        int resultLimit = limit == null ? 20 : limit;
        if (resultLimit < 1 || resultLimit > 20) {
            throw new IllegalArgumentException("limit must be between 1 and 20");
        }
        return semanticSearchService.search(query, resultLimit);
    }

    @McpTool(
        name = "search_active_jobs",
        description = "Search active job postings by keyword with optional location and employment-type filters. "
            + "Returns at most 20 matches, newest first. Use this when looking for current jobs to recommend.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public JobSearchResult searchActiveJobs(
        @McpToolParam(
            description = "Keywords to match against title, company, or description, e.g. 'senior java backend'",
            required = true
        ) String query,
        @McpToolParam(description = "Location substring, e.g. 'Nairobi' or 'Remote'; omit for any location", required = false)
        String location,
        @McpToolParam(description = "Employment type such as FULL_TIME or CONTRACT; omit for any type", required = false)
        EmploymentType employmentType,
        @McpToolParam(description = "Maximum results to return, from 1 to 20; defaults to 20", required = false)
        Integer limit
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank; pass keywords such as 'senior java backend'");
        }
        int resultLimit = limit == null ? 20 : limit;
        if (resultLimit < 1 || resultLimit > 20) {
            throw new IllegalArgumentException("limit must be between 1 and 20");
        }
        var pageable = PageRequest.of(0, resultLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jobSearchService.search(query.trim(), location, employmentType, pageable);
    }

    @McpTool(
        name = "get_job_details",
        description = "Get full details and original source/application links for one job. "
            + "Requires the job UUID returned by search_active_jobs; use this before giving a user an application link.",
        annotations = @McpTool.McpAnnotations(
            readOnlyHint = true,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    public JobDetailResult getJobDetails(
        @McpToolParam(description = "Job UUID returned by search_active_jobs", required = true) String jobId
    ) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must be a UUID returned by search_active_jobs");
        }
        try {
            return jobSearchService.getJob(UUID.fromString(jobId));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("jobId must be a UUID returned by search_active_jobs");
        }
    }
}
