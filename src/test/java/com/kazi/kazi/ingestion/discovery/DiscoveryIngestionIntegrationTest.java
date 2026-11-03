package com.kazi.kazi.ingestion.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseJobSource;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseProperties;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "discovery.sites[0].company=Acme",
    "discovery.sites[0].url=https://93.184.216.34/careers"
})
class DiscoveryIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DiscoveryIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private DiscoveryHttpClient discoveryHttpClient;

    @MockitoBean
    private GreenhouseJobSource greenhouseJobSource;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_discover_route_and_ingest_a_career_page_board() {
        URI page = URI.create("https://93.184.216.34/careers");
        when(discoveryHttpClient.getPage(page)).thenReturn(
            "<html><a href=\"https://boards.greenhouse.io/acme\">Careers</a></html>"
        );
        when(greenhouseJobSource.fetchSince(any(GreenhouseProperties.Board.class), isNull())).thenReturn(List.of(
            new RawJob(
                "greenhouse:acme",
                "job-1",
                "https://boards.greenhouse.io/acme/jobs/1",
                "Java Engineer",
                "Acme",
                "Nairobi, Kenya",
                null,
                "Full-time",
                "Build Spring services",
                Instant.parse("2026-11-02T10:00:00Z"),
                Map.of("greenhouse", Map.of("id", "job-1"))
            )
        ));

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll()).singleElement().satisfies(run -> {
            assertThat(run.getSourceName()).isEqualTo("greenhouse:acme");
            assertThat(run.getStatus()).isEqualTo(SourceRunStatus.COMPLETED);
            assertThat(run.getJobsFetched()).isEqualTo(1);
        });
        assertThat(jobRepository.findAll()).singleElement();
        assertThat(sourceLinkRepository.findAll()).singleElement();
    }
}
