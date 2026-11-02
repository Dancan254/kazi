package com.kazi.kazi.mcp;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.search.JobSearchService;
import com.kazi.kazi.search.JobSearchService.JobSearchResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JobMcpTools {

    private final JobSearchService jobSearchService;

    JobMcpTools(JobSearchService jobSearchService) {
        this.jobSearchService = jobSearchService;
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
}
