package com.kazi.kazi.ingestion.greenhouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.greenhouse.GreenhouseProperties.Board;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class GreenhouseJobSourceTest {

    private final GreenhouseJobSource jobSource;
    private final MockRestServiceServer server;

    GreenhouseJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost").build();
        GreenhouseHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(GreenhouseHttpClient.class);
        jobSource = new GreenhouseJobSource(httpClient);
    }

    @Test
    void should_map_board_jobs_and_filter_by_update_date() {
        server.expect(requestTo(containsString("/acme/jobs")))
            .andExpect(queryParam("content", "true"))
            .andRespond(withSuccess("""
                {
                  "jobs": [
                    {
                      "id": 12345,
                      "requisition_id": "REQ-1",
                      "title": "Senior Java Engineer",
                      "location": {"name": "Nairobi, Kenya"},
                      "content": "<p>Build Spring services</p>",
                      "absolute_url": "https://boards.greenhouse.io/acme/jobs/12345",
                      "updated_at": "2026-11-02T10:00:00Z"
                    },
                    {
                      "id": 67890,
                      "title": "Old role",
                      "absolute_url": "https://boards.greenhouse.io/acme/jobs/67890",
                      "updated_at": "2026-10-01T10:00:00Z"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(
            new Board("Acme", "acme"),
            Instant.parse("2026-11-01T00:00:00Z")
        );

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("greenhouse:acme");
        assertThat(job.sourceJobId()).isEqualTo("12345");
        assertThat(job.sourceUrl()).isEqualTo("https://boards.greenhouse.io/acme/jobs/12345");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Nairobi, Kenya");
        assertThat(job.description()).isEqualTo("<p>Build Spring services</p>");
        assertThat(job.postedAt()).isNull();
        assertThat(job.rawData()).containsKey("greenhouse");
        server.verify();
    }
}
