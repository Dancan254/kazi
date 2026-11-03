package com.kazi.kazi.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.normalization.JobNormalizer;
import com.kazi.kazi.ingestion.normalization.NormalizedJob;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JobServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    private final JobNormalizer normalizer = new JobNormalizer();

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void should_insert_canonical_job_and_source_provenance() {
        var savedJob = jobService.save(normalizedJob("adzuna", "Description", "USD 120000 - 150000"));

        assertThat(savedJob.inserted()).isTrue();
        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(sourceLinkRepository.findAll()).hasSize(1);
        Job job = jobRepository.findAll().getFirst();
        JobSourceLink source = sourceLinkRepository.findAll().getFirst();
        assertThat(job.getDeterministicKey()).hasSize(64);
        assertThat(job.getEmploymentType()).isEqualTo(NormalizedJob.EmploymentType.FULL_TIME);
        assertThat(source.getSourceName()).isEqualTo("adzuna");
        assertThat(source.getRawData()).containsEntry("id", "adzuna-1");
    }

    @Test
    void should_merge_duplicate_and_add_second_source() {
        jobService.save(normalizedJob("adzuna", "Short", "USD 120000 - 150000"));

        var savedJob = jobService.save(normalizedJob(
            "remoteok",
            "A much longer description of the same role",
            "USD 100000 - 160000"
        ));

        assertThat(savedJob.inserted()).isFalse();
        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(sourceLinkRepository.findAll()).hasSize(2);
        Job job = jobRepository.findAll().getFirst();
        assertThat(job.getDescription()).isEqualTo("A much longer description of the same role");
        assertThat(job.getSalaryMin()).isEqualTo(100_000);
        assertThat(job.getSalaryMax()).isEqualTo(160_000);
    }

    @Test
    void should_update_existing_source_instead_of_duplicating_it() {
        jobService.save(normalizedJob("adzuna", "Description", null));
        jobService.save(normalizedJob("adzuna", "Description", null));

        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(sourceLinkRepository.findAll()).hasSize(1);
    }

    @Test
    void should_deactivate_job_only_after_all_sources_are_missing() {
        jobService.save(normalizedJob("adzuna", "Description", null));
        jobService.save(normalizedJob("greenhouse", "Description", null));

        jobService.deactivateMissing("adzuna", Set.of());

        Job job = jobRepository.findAll().getFirst();
        assertThat(job.getStatus()).isEqualTo("active");
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::isActive)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("adzuna", false),
                org.assertj.core.groups.Tuple.tuple("greenhouse", true)
            );

        jobService.deactivateMissing("greenhouse", Set.of());

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");
        assertThat(sourceLinkRepository.findAll()).allMatch(source -> !source.isActive());
    }

    @Test
    void should_reactivate_job_when_a_source_returns_it() {
        jobService.save(normalizedJob("adzuna", "Description", null));
        jobService.deactivateMissing("adzuna", Set.of());
        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");

        jobService.save(normalizedJob("adzuna", "Description", null));

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isTrue();
    }

    private NormalizedJob normalizedJob(String sourceName, String description, String salary) {
        return normalizer.normalize(new RawJob(
            sourceName,
            sourceName + "-1",
            "https://example.com/" + sourceName + "/job-1",
            "Sr. SWE (Backend)",
            "JPMorgan Chase & Co.",
            "Nairobi, Kenya (Remote)",
            salary,
            "Full Time",
            description,
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", sourceName + "-1")
        ));
    }
}
