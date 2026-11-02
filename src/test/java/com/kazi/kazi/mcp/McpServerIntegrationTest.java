package com.kazi.kazi.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.normalization.JobNormalizer;
import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobService;
import com.kazi.kazi.job.JobSourceLinkRepository;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

class McpServerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

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
    void should_list_and_call_search_active_jobs_tool() {
        saveJavaJob();

        try (var client = client()) {
            client.initialize();

            assertThat(client.listTools().tools())
                .extracting(McpSchema.Tool::name)
                .contains("search_active_jobs", "get_job_details");

            var result = client.callTool(new McpSchema.CallToolRequest(
                "search_active_jobs",
                Map.of("query", "java", "limit", 5)
            ));

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).isNotEmpty();
        }
    }

    @Test
    void should_return_job_details_with_source_links() {
        UUID jobId = saveJavaJob();

        try (var client = client()) {
            client.initialize();

            var result = client.callTool(new McpSchema.CallToolRequest(
                "get_job_details",
                Map.of("jobId", jobId.toString())
            ));

            assertThat(result.isError()).isFalse();
            var content = (McpSchema.TextContent) result.content().getFirst();
            assertThat(content.text()).contains("https://example.com/jobs/mcp-job-1");
        }
    }

    @Test
    void should_return_error_result_when_limit_is_invalid() {
        try (var client = client()) {
            client.initialize();

            var result = client.callTool(new McpSchema.CallToolRequest(
                "search_active_jobs",
                Map.of("query", "java", "limit", 21)
            ));

            assertThat(result.isError()).isTrue();
        }
    }

    @Test
    void should_return_error_result_when_job_does_not_exist() {
        try (var client = client()) {
            client.initialize();

            var result = client.callTool(new McpSchema.CallToolRequest(
                "get_job_details",
                Map.of("jobId", UUID.randomUUID().toString())
            ));

            assertThat(result.isError()).isTrue();
        }
    }

    private io.modelcontextprotocol.client.McpSyncClient client() {
        var transport = HttpClientStreamableHttpTransport
            .builder("http://localhost:" + port + "/mcp")
            .build();
        return McpClient.sync(transport).build();
    }

    private UUID saveJavaJob() {
        return jobService.save(normalizer.normalize(new RawJob(
            "test-source",
            "mcp-job-1",
            "https://example.com/jobs/mcp-job-1",
            "Java Developer",
            "Acme",
            "Nairobi, Kenya",
            null,
            "Full Time",
            "Spring Boot",
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", "mcp-job-1")
        ))).id();
    }
}
