package com.kazi.kazi.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IngestionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void cleanRuns() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_save_completed_run_with_fetched_job_count() {
        var run = ingestionService.ingest(healthySource("test-source"));

        assertThat(run.getStatus()).isEqualTo(SourceRunStatus.COMPLETED);
        assertThat(run.getJobsFetched()).isEqualTo(1);
        assertThat(run.getJobsInserted()).isEqualTo(1);
        assertThat(sourceRunRepository.findById(run.getId())).isPresent();
    }

    @Test
    void should_continue_to_next_source_after_a_failure() {
        var runs = ingestionService.ingestAll(List.of(failingSource(), healthySource("healthy")));

        assertThat(runs).extracting(SourceRun::getStatus)
            .containsExactly(SourceRunStatus.FAILED, SourceRunStatus.COMPLETED);
        assertThat(runs.getFirst().getErrorMessage()).isEqualTo("timeout");
        assertThat(sourceRunRepository.findAll()).hasSize(2);
    }

    private JobSource healthySource(String name) {
        return new JobSource() {
            @Override
            public String sourceName() {
                return name;
            }

            @Override
            public List<RawJob> fetchSince(Instant lastRun) {
                return List.of(new RawJob(
                    name,
                    "job-1",
                    "https://example.com/job-1",
                    "Java Developer",
                    "Acme",
                    "Remote",
                    null,
                    "Full Time",
                    "Description",
                    Instant.parse("2026-11-01T00:00:00Z"),
                    Map.of("id", "job-1")
                ));
            }
        };
    }

    private JobSource failingSource() {
        return new JobSource() {
            @Override
            public String sourceName() {
                return "failing";
            }

            @Override
            public List<RawJob> fetchSince(Instant lastRun) {
                throw new IllegalStateException("timeout");
            }
        };
    }
}
