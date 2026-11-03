package com.kazi.kazi.ingestion.workable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.workable.WorkableResponse.WorkableJob;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "workable.accounts[0].company=Acme",
    "workable.accounts[0].account=acme",
    "workable.accounts[1].company=Beta",
    "workable.accounts[1].account=beta",
    "workable.retry.max-retries=0"
})
class WorkableIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WorkableIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private WorkableHttpClient httpClient;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_account_with_distinct_provenance() {
        stubAccount("acme", "JOB1", "Java Engineer");
        stubAccount("beta", "JOB2", "Platform Engineer");

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName)
            .containsExactlyInAnyOrder("workable:acme", "workable:beta");
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("workable:acme", "JOB1"),
                org.assertj.core.groups.Tuple.tuple("workable:beta", "JOB2")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    @Test
    void should_deactivate_a_job_removed_from_the_account() {
        stubAccount("acme", "JOB1", "Java Engineer");
        stubEmptyAccount("beta");
        scheduler.ingest();
        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");

        stubEmptyAccount("acme");
        scheduler.ingest();

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isFalse();
    }

    private void stubAccount(String account, String id, String title) {
        when(httpClient.getJobs(account, true)).thenReturn(new WorkableResponse(account, List.of(new WorkableJob(
            title,
            id,
            "Full-time",
            false,
            "Nairobi",
            null,
            "Kenya",
            LocalDate.parse("2026-11-02"),
            "https://apply.workable.com/j/" + id + "/apply",
            "https://apply.workable.com/j/" + id,
            "<p>Build Spring services</p>",
            null
        ))));
    }

    private void stubEmptyAccount(String account) {
        when(httpClient.getJobs(account, true)).thenReturn(new WorkableResponse(account, List.of()));
    }
}
