package com.kazi.kazi.ingestion.lever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.lever.LeverProperties.Site;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class LeverJobSourceTest {

    private final LeverJobSource jobSource;
    private final MockRestServiceServer server;

    LeverJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost").build();
        LeverHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(LeverHttpClient.class);
        jobSource = new LeverJobSource(httpClient, new LeverProperties("http://localhost", 2, null));
    }

    @Test
    void should_fetch_all_pages_and_map_lever_postings() {
        server.expect(requestTo(containsString("/acme")))
            .andExpect(queryParam("skip", "0"))
            .andExpect(queryParam("limit", "2"))
            .andExpect(queryParam("mode", "json"))
            .andRespond(withSuccess("""
                [
                  {
                    "id": "job-1",
                    "text": "Senior Java Engineer",
                    "categories": {"commitment": "Full Time", "location": "San Francisco"},
                    "createdAt": 1767225600000,
                    "description": "<p>Build Spring services</p>",
                    "lists": [{"text": "Requirements", "content": "<ul><li>Java</li></ul>"}],
                    "additional": "<p>Apply to join us</p>",
                    "hostedUrl": "https://jobs.lever.co/acme/job-1",
                    "applyUrl": "https://jobs.lever.co/acme/job-1/apply",
                    "workplaceType": "remote",
                    "salaryRange": {"min": 120000, "max": 150000, "currency": "USD", "interval": "per-year-salary"}
                  },
                  {
                    "id": "old-job",
                    "text": "Old role",
                    "createdAt": 1735689600000
                  }
                ]
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/acme")))
            .andExpect(queryParam("skip", "2"))
            .andExpect(queryParam("limit", "2"))
            .andExpect(queryParam("mode", "json"))
            .andRespond(withSuccess("""
                [
                  {
                    "id": "job-2",
                    "text": "Platform Engineer",
                    "categories": {"location": "Nairobi, Kenya"},
                    "createdAt": 1767312000000,
                    "descriptionPlain": "Build platforms",
                    "hostedUrl": "https://jobs.lever.co/acme/job-2"
                  }
                ]
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(
            new Site("Acme", "acme"),
            Instant.parse("2025-12-01T00:00:00Z")
        );

        assertThat(jobs).hasSize(2);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("lever:acme");
        assertThat(job.sourceJobId()).isEqualTo("job-1");
        assertThat(job.sourceUrl()).isEqualTo("https://jobs.lever.co/acme/job-1/apply");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("San Francisco, Remote");
        assertThat(job.salary()).isEqualTo("USD 120000 - 150000 per-year-salary");
        assertThat(job.employmentType()).isEqualTo("Full Time");
        assertThat(job.description()).contains("Build Spring services", "Requirements", "Java", "Apply to join us");
        assertThat(job.postedAt()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(job.rawData()).containsKey("lever");
        server.verify();
    }
}
