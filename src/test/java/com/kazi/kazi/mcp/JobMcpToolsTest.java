package com.kazi.kazi.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kazi.kazi.search.JobSearchService;
import com.kazi.kazi.search.SemanticSearchService;
import com.kazi.kazi.search.JobSearchService.JobSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class JobMcpToolsTest {

    private final JobSearchService jobSearchService = mock(JobSearchService.class);
    private final SemanticSearchService semanticSearchService = mock(SemanticSearchService.class);
    private final JobMcpTools jobMcpTools = new JobMcpTools(jobSearchService, semanticSearchService);

    @Test
    void should_delegate_semantic_search_with_requested_limit() {
        var expected = new JobSearchResult(List.of(), 0, 5, 0, 0);
        when(semanticSearchService.search("java backend", 5)).thenReturn(expected);

        var result = jobMcpTools.semanticSearchJobs("java backend", 5);

        assertThat(result).isSameAs(expected);
        verify(semanticSearchService).search("java backend", 5);
    }

    @Test
    void should_delegate_valid_search_with_requested_limit() {
        var expected = new JobSearchResult(List.of(), 0, 5, 0, 0);
        when(jobSearchService.search(eq("java"), isNull(), isNull(), any(Pageable.class))).thenReturn(expected);

        var result = jobMcpTools.searchActiveJobs(" java ", null, null, 5);

        assertThat(result).isSameAs(expected);
        verify(jobSearchService).search(
            eq("java"),
            isNull(),
            isNull(),
            org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 5 && pageable.getPageNumber() == 0)
        );
    }

    @Test
    void should_reject_blank_query_with_actionable_message() {
        assertThatThrownBy(() -> jobMcpTools.searchActiveJobs(" ", null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("query must not be blank; pass keywords such as 'senior java backend'");
    }

    @Test
    void should_reject_limit_above_twenty() {
        assertThatThrownBy(() -> jobMcpTools.searchActiveJobs("java", null, null, 21))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("limit must be between 1 and 20");
    }

    @Test
    void should_delegate_valid_job_details_request() {
        var jobId = java.util.UUID.randomUUID();
        var expected = mock(JobSearchService.JobDetailResult.class);
        when(jobSearchService.getJob(jobId)).thenReturn(expected);

        var result = jobMcpTools.getJobDetails(jobId.toString());

        assertThat(result).isSameAs(expected);
        verify(jobSearchService).getJob(jobId);
    }

    @Test
    void should_reject_malformed_job_id_with_actionable_message() {
        assertThatThrownBy(() -> jobMcpTools.getJobDetails("not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("jobId must be a UUID returned by search_active_jobs");
    }
}
