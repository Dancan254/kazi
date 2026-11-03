package com.kazi.kazi.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.normalization.JobNormalizer;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobService;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@AutoConfigureRestTestClient
@TestPropertySource(properties = "kazi.semantic.enabled=true")
class SemanticSearchIntegrationTest extends BaseIntegrationTest {

    @Autowired private RestTestClient client;
    @Autowired private JobService jobService;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobSourceLinkRepository sourceLinkRepository;
    @Autowired private SemanticSearchService semanticSearchService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private EmbeddingModel embeddingModel;

    private final JobNormalizer normalizer = new JobNormalizer();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM job_embedding");
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        when(embeddingModel.embed(anyString())).thenAnswer(invocation -> {
            String content = invocation.getArgument(0, String.class).toLowerCase();
            return content.contains("java") ? vector(0) : vector(1);
        });
    }

    @Test
    void should_return_semantically_closest_active_job_over_http() {
        UUID javaJob = saveJob("java-1", "Senior Java Engineer", "Build Spring services");
        saveJob("sales-1", "Account Executive", "Sell enterprise contracts");

        semanticSearchService.index(javaJob);
        semanticSearchService.index(jobRepository.findAll().stream()
            .filter(job -> !job.getId().equals(javaJob))
            .findFirst()
            .orElseThrow()
            .getId());

        client.get().uri("/api/jobs/search/semantic?q=java%20backend&limit=1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(1)
            .jsonPath("$.content[0].id").isEqualTo(javaJob.toString())
            .jsonPath("$.content[0].title").isEqualTo("senior java engineer");
    }

    @Test
    void should_not_embed_unchanged_job_twice() {
        UUID jobId = saveJob("java-1", "Senior Java Engineer", "Build Spring services");

        semanticSearchService.index(jobId);
        semanticSearchService.index(jobId);

        verify(embeddingModel, times(1)).embed(anyString());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM job_embedding", Long.class)).isEqualTo(1);
    }

    @Test
    void should_exclude_inactive_jobs_from_semantic_results() {
        UUID jobId = saveJob("java-1", "Senior Java Engineer", "Build Spring services");
        semanticSearchService.index(jobId);
        jobService.deactivateMissing("test-source", Set.of());

        assertThat(semanticSearchService.search("java", 20).content()).isEmpty();
    }

    private float[] vector(int oneIndex) {
        float[] vector = new float[768];
        vector[oneIndex] = 1;
        return vector;
    }

    private UUID saveJob(String id, String title, String description) {
        return jobService.save(normalizer.normalize(new RawJob(
            "test-source",
            id,
            "https://example.com/jobs/" + id,
            title,
            "Acme",
            "Nairobi, Kenya",
            null,
            "Full Time",
            description,
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", id)
        ))).id();
    }
}
