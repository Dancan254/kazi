package com.kazi.kazi.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
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

    @Test
    void should_deactivate_jobs_missing_from_a_full_snapshot() {
        SnapshotSource source = new SnapshotSource();
        source.jobs = List.of(rawJob("snapshot", "job-1"), rawJob("snapshot", "job-2"));
        ingestionService.ingest(source);

        source.jobs = List.of(rawJob("snapshot", "job-1"));
        ingestionService.ingest(source);

        assertThat(jobRepository.findAll())
            .extracting(Job::getTitle, Job::getStatus)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("java developer job 1", "active"),
                org.assertj.core.groups.Tuple.tuple("java developer job 2", "inactive")
            );
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceJobId, JobSourceLink::isActive)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("job-1", true),
                org.assertj.core.groups.Tuple.tuple("job-2", false)
            );
    }

    @Test
    void should_keep_jobs_active_when_a_snapshot_fetch_fails() {
        SnapshotSource source = new SnapshotSource();
        source.jobs = List.of(rawJob("snapshot", "job-1"));
        ingestionService.ingest(source);

        source.fail = true;
        ingestionService.ingest(source);

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isTrue();
    }

    private RawJob rawJob(String sourceName, String id) {
        return new RawJob(
            sourceName,
            id,
            "https://example.com/" + id,
            "Java Developer " + id,
            "Acme",
            "Remote",
            null,
            "Full Time",
            "Description",
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", id)
        );
    }

    private static class SnapshotSource implements JobSource {
        private List<RawJob> jobs = List.of();
        private boolean fail;

        @Override
        public String sourceName() {
            return "snapshot";
        }

        @Override
        public List<RawJob> fetchSince(Instant lastRun) {
            if (fail) {
                throw new IllegalStateException("timeout");
            }
            return jobs;
        }

        @Override
        public boolean providesFullSnapshot() {
            return true;
        }
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
