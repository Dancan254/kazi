package com.kazi.kazi.search;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.normalization.JobNormalizer;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobService;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;

@AutoConfigureRestTestClient
class JobSearchApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RestTestClient client;

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
    void should_return_only_jobs_matching_all_filters() {
        saveJob("1", "Senior Java Developer", "Acme", "Nairobi, Kenya", "Full Time", "Spring Boot");
        saveJob("2", "Java Developer", "Acme", "Mombasa, Kenya", "Full Time", "Spring Boot");
        saveJob("3", "Python Developer", "Acme", "Nairobi, Kenya", "Full Time", "Django");
        saveJob("4", "Java Developer", "Acme", "Nairobi, Kenya", "Part Time", "Spring Boot");

        client.get().uri("/api/jobs?q=java&location=Nairobi&employmentType=FULL_TIME")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(1)
            .jsonPath("$.content[0].title").isEqualTo("senior java developer")
            .jsonPath("$.content[0].employmentType").isEqualTo("FULL_TIME")
            .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void should_match_keyword_case_insensitively_against_company_and_description() {
        saveJob("1", "Platform Engineer", "Acme", "Remote", "Contract", "We use Java and Kafka");
        saveJob("2", "Data Engineer", "JavaHouse", "Nairobi, Kenya", "Full Time", "Python and Airflow");
        saveJob("3", "Frontend Engineer", "Other", "Remote", "Contract", "React and TypeScript");

        client.get().uri("/api/jobs?q=JAVA")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(2)
            .jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void should_return_requested_page_and_metadata() {
        saveJob("1", "Java Developer", "Acme", "Remote", "Full Time", "Spring Boot");
        saveJob("2", "Kotlin Developer", "Acme", "Remote", "Full Time", "Spring Boot");
        saveJob("3", "Platform Engineer", "Acme", "Remote", "Full Time", "Kubernetes");

        client.get().uri("/api/jobs?page=1&size=2")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(1)
            .jsonPath("$.page").isEqualTo(1)
            .jsonPath("$.size").isEqualTo(2)
            .jsonPath("$.totalElements").isEqualTo(3)
            .jsonPath("$.totalPages").isEqualTo(2);
    }

    private void saveJob(
        String sourceJobId,
        String title,
        String company,
        String location,
        String employmentType,
        String description
    ) {
        jobService.save(normalizer.normalize(new RawJob(
            "test-source",
            sourceJobId,
            "https://example.com/jobs/" + sourceJobId,
            title,
            company,
            location,
            null,
            employmentType,
            description,
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", sourceJobId)
        )));
    }
}
