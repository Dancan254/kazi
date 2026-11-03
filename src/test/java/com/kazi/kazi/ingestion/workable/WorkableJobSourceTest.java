package com.kazi.kazi.ingestion.workable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.workable.WorkableProperties.Account;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class WorkableJobSourceTest {

    private final WorkableJobSource jobSource;
    private final MockRestServiceServer server;

    WorkableJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost").build();
        WorkableHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(WorkableHttpClient.class);
        jobSource = new WorkableJobSource(httpClient);
    }

    @Test
    void should_map_workable_jobs() {
        server.expect(requestTo(containsString("/acme")))
            .andExpect(queryParam("details", "true"))
            .andRespond(withSuccess("""
                {
                  "name": "Acme",
                  "jobs": [{
                    "title": "Senior Java Engineer",
                    "shortcode": "ABC123",
                    "employment_type": "Full-time",
                    "telecommuting": true,
                    "city": "Paris",
                    "state": "Île-de-France",
                    "country": "France",
                    "published_on": "2026-11-02",
                    "application_url": "https://apply.workable.com/j/ABC123/apply",
                    "url": "https://apply.workable.com/j/ABC123",
                    "description": "<p>Build Spring services</p>",
                    "locations": [{"city": "Paris", "region": "Île-de-France", "country": "France"}]
                  }]
                }
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(new Account("Acme", "acme"), null);

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("workable:acme");
        assertThat(job.sourceJobId()).isEqualTo("ABC123");
        assertThat(job.sourceUrl()).isEqualTo("https://apply.workable.com/j/ABC123/apply");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Paris, Île-de-France, France, Remote");
        assertThat(job.employmentType()).isEqualTo("Full-time");
        assertThat(job.description()).isEqualTo("<p>Build Spring services</p>");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T00:00:00Z");
        assertThat(job.rawData()).containsKey("workable");
        server.verify();
    }
}
