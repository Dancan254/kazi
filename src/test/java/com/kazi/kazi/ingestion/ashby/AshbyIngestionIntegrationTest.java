package com.kazi.kazi.ingestion.ashby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.ashby.AshbyResponse.AshbyJob;
import com.kazi.kazi.ingestion.ashby.AshbyResponse.Compensation;
import com.kazi.kazi.ingestion.ashby.AshbyResponse.SecondaryLocation;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
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
    "ashby.organizations[0].company=Acme",
    "ashby.organizations[0].board=acme",
    "ashby.organizations[1].company=Beta",
    "ashby.organizations[1].board=beta",
    "ashby.retry.max-retries=0"
})
class AshbyIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AshbyIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private AshbyHttpClient httpClient;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_organization_with_distinct_provenance() {
        when(httpClient.getJobs("acme", true)).thenReturn(response("job-1", "Java Engineer"));
        when(httpClient.getJobs("beta", true)).thenReturn(response("job-2", "Platform Engineer"));

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName, SourceRun::getStatus)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("ashby:acme", SourceRunStatus.COMPLETED),
                org.assertj.core.groups.Tuple.tuple("ashby:beta", SourceRunStatus.COMPLETED)
            );
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("ashby:acme", "job-1"),
                org.assertj.core.groups.Tuple.tuple("ashby:beta", "job-2")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    private AshbyResponse response(String id, String title) {
        return new AshbyResponse("1", List.of(new AshbyJob(
            id,
            title,
            "Europe",
            List.of(new SecondaryLocation("Nairobi, Kenya")),
            "FullTime",
            "Remote",
            "<p>Build Spring services</p>",
            "Build Spring services",
            Instant.parse("2026-11-02T10:00:00Z"),
            "https://jobs.ashbyhq.com/example/" + id,
            "https://jobs.ashbyhq.com/example/" + id + "/application",
            new Compensation("$120K - $150K", "$120K - $150K")
        )));
    }
}
