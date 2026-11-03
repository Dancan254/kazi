package com.kazi.kazi.ingestion.smartrecruiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersPostingsResponse.Posting;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "smartrecruiters.companies[0].company=Acme",
    "smartrecruiters.companies[0].identifier=acme",
    "smartrecruiters.companies[1].company=Beta",
    "smartrecruiters.companies[1].identifier=beta",
    "smartrecruiters.retry.max-retries=0"
})
class SmartRecruitersIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SmartRecruitersIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private SmartRecruitersClient client;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_company_with_distinct_provenance() {
        stubCompany("acme", "job-1", "Java Engineer");
        stubCompany("beta", "job-2", "Platform Engineer");

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName)
            .containsExactlyInAnyOrder("smartrecruiters:acme", "smartrecruiters:beta");
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("smartrecruiters:acme", "job-1"),
                org.assertj.core.groups.Tuple.tuple("smartrecruiters:beta", "job-2")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    @Test
    void should_deactivate_a_job_removed_from_the_company() {
        stubCompany("acme", "job-1", "Java Engineer");
        stubEmptyCompany("beta");
        scheduler.ingest();
        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");

        stubEmptyCompany("acme");
        scheduler.ingest();

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isFalse();
    }

    private void stubCompany(String identifier, String id, String title) {
        when(client.getPostings(identifier, 100, 0)).thenReturn(
            new SmartRecruitersPostingsResponse(0, 100, 1, List.of(new Posting(
                id, title, null, null, null, Instant.parse("2026-11-02T10:00:00Z")
            )))
        );
        when(client.getPosting(identifier, id)).thenReturn(new SmartRecruitersPostingResponse(
            id,
            title,
            null,
            new SmartRecruitersPostingsResponse.Location("Nairobi", null, "Kenya", false, false, "Nairobi, Kenya"),
            new SmartRecruitersPostingsResponse.EmploymentType("permanent", "Full-time"),
            Instant.parse("2026-11-02T10:00:00Z"),
            "https://jobs.smartrecruiters.com/" + identifier + "/" + id,
            "https://jobs.smartrecruiters.com/" + identifier + "/" + id + "?oga=true",
            null
        ));
    }

    private void stubEmptyCompany(String identifier) {
        when(client.getPostings(identifier, 100, 0)).thenReturn(
            new SmartRecruitersPostingsResponse(0, 100, 0, List.of())
        );
    }
}
