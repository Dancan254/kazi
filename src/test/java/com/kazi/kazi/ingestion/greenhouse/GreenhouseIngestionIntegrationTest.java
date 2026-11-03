package com.kazi.kazi.ingestion.greenhouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseResponse.GreenhouseJob;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseResponse.Location;
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
    "greenhouse.boards[0].company=Acme",
    "greenhouse.boards[0].token=acme",
    "greenhouse.boards[1].company=Beta",
    "greenhouse.boards[1].token=beta",
    "greenhouse.retry.max-retries=0"
})
class GreenhouseIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GreenhouseIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private GreenhouseHttpClient httpClient;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_board_with_distinct_provenance() {
        when(httpClient.getJobs("acme", true)).thenReturn(response(12345L, "Java Engineer"));
        when(httpClient.getJobs("beta", true)).thenReturn(response(67890L, "Platform Engineer"));

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName, SourceRun::getStatus)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("greenhouse:acme", SourceRunStatus.COMPLETED),
                org.assertj.core.groups.Tuple.tuple("greenhouse:beta", SourceRunStatus.COMPLETED)
            );
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("greenhouse:acme", "12345"),
                org.assertj.core.groups.Tuple.tuple("greenhouse:beta", "67890")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    @Test
    void should_deactivate_a_job_removed_from_the_board() {
        when(httpClient.getJobs("acme", true)).thenReturn(response(12345L, "Java Engineer"));
        when(httpClient.getJobs("beta", true)).thenReturn(new GreenhouseResponse(List.of()));
        scheduler.ingest();
        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");

        when(httpClient.getJobs("acme", true)).thenReturn(new GreenhouseResponse(List.of()));
        scheduler.ingest();

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isFalse();
    }

    private GreenhouseResponse response(Long id, String title) {
        return new GreenhouseResponse(List.of(new GreenhouseJob(
            id,
            title,
            new Location("Nairobi, Kenya"),
            "<p>Build Spring services</p>",
            "https://boards.greenhouse.io/example/jobs/" + id,
            Instant.parse("2026-11-02T10:00:00Z")
        )));
    }
}
