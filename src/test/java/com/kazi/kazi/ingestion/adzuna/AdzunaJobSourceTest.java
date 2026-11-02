package com.kazi.kazi.ingestion.adzuna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class AdzunaJobSourceTest {

    private final AdzunaJobSource jobSource;
    private final MockRestServiceServer server;

    AdzunaJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost").build();
        AdzunaHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(AdzunaHttpClient.class);
        jobSource = new AdzunaJobSource(
            httpClient,
            new AdzunaProperties("http://localhost", "test-id", "test-key", "us", "USD", 50)
        );
    }

    @Test
    void should_map_adzuna_jobs_and_filter_by_creation_date() {
        server.expect(requestTo(containsString("/jobs/us/search/1")))
            .andExpect(queryParam("app_id", "test-id"))
            .andExpect(queryParam("app_key", "test-key"))
            .andExpect(queryParam("results_per_page", "50"))
            .andRespond(withSuccess("""
                {
                  "results": [
                    {
                      "id": "job-1",
                      "title": "Senior Java Engineer",
                      "company": {"display_name": "Acme"},
                      "location": {"display_name": "Austin, Texas"},
                      "salary_min": 120000,
                      "salary_max": 150000,
                      "contract_type": "permanent",
                      "contract_time": "full_time",
                      "description": "Build Spring services",
                      "redirect_url": "https://example.com/job-1",
                      "created": "2026-11-02T10:00:00Z"
                    },
                    {
                      "id": "old-job",
                      "title": "Old role",
                      "created": "2026-10-01T10:00:00Z"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(Instant.parse("2026-11-01T00:00:00Z"));

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("adzuna");
        assertThat(job.sourceJobId()).isEqualTo("job-1");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Austin, Texas");
        assertThat(job.salary()).isEqualTo("USD 120000 - 150000");
        assertThat(job.employmentType()).isEqualTo("full_time");
        assertThat(job.rawData()).containsKey("adzuna");
        server.verify();
    }
}
