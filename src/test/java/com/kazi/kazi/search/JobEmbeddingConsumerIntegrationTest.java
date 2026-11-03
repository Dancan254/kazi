package com.kazi.kazi.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.normalization.JobNormalizer;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.JobEventPublisher;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobService;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = "kazi.semantic.enabled=true")
class JobEmbeddingConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired private JobService jobService;
    @Autowired private JobEventPublisher eventPublisher;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobSourceLinkRepository sourceLinkRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private EmbeddingModel embeddingModel;

    private final JobNormalizer normalizer = new JobNormalizer();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM job_embedding");
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> new float[768]);
    }

    @Test
    void should_index_new_and_merged_jobs_from_kafka_events() {
        var indexed = saveJob("indexed-1", "Java Engineer");
        saveJob("base-1", "Platform Engineer");
        var merged = saveJob("merged-1", "Platform Engineer");

        eventPublisher.publish(indexed, UUID.randomUUID());
        eventPublisher.publish(merged, UUID.randomUUID());

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM job_embedding", Long.class))
                .isEqualTo(2)
        );
        assertThat(jdbcTemplate.queryForList("SELECT job_id FROM job_embedding", UUID.class))
            .containsExactlyInAnyOrder(indexed.id(), merged.id());
    }

    private com.kazi.kazi.job.JobService.SavedJob saveJob(String id, String title) {
        return jobService.save(normalizer.normalize(new RawJob(
            "test-source",
            id,
            "https://example.com/jobs/" + id,
            title,
            "Acme",
            "Remote",
            null,
            "Full Time",
            "Build distributed systems",
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", id)
        )));
    }
}
