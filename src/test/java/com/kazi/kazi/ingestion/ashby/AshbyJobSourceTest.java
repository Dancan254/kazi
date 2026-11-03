package com.kazi.kazi.ingestion.ashby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.ashby.AshbyProperties.Organization;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class AshbyJobSourceTest {

    private final AshbyJobSource jobSource;
    private final MockRestServiceServer server;

    AshbyJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost").build();
        AshbyHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(AshbyHttpClient.class);
        jobSource = new AshbyJobSource(httpClient);
    }

    @Test
    void should_map_ashby_jobs_and_filter_by_publication_date() {
        server.expect(requestTo(containsString("/acme")))
            .andExpect(queryParam("includeCompensation", "true"))
            .andRespond(withSuccess("""
                {
                  "apiVersion": "1",
                  "jobs": [
                    {
                      "id": "job-1",
                      "title": "Senior Java Engineer",
                      "location": "Europe",
                      "secondaryLocations": [{"location": "Nairobi, Kenya"}],
                      "employmentType": "FullTime",
                      "workplaceType": "Remote",
                      "descriptionHtml": "<p>Build Spring services</p>",
                      "descriptionPlain": "Build Spring services",
                      "publishedAt": "2026-11-02T10:00:00+00:00",
                      "jobUrl": "https://jobs.ashbyhq.com/acme/job-1",
                      "applyUrl": "https://jobs.ashbyhq.com/acme/job-1/application",
                      "compensation": {"scrapeableCompensationSalarySummary": "€150K - €250K"}
                    },
                    {
                      "id": "old-job",
                      "title": "Old role",
                      "publishedAt": "2026-10-01T10:00:00+00:00"
                    }
                  ]
                }
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(
            new Organization("Acme", "acme"),
            Instant.parse("2026-11-01T00:00:00Z")
        );

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("ashby:acme");
        assertThat(job.sourceJobId()).isEqualTo("job-1");
        assertThat(job.sourceUrl()).isEqualTo("https://jobs.ashbyhq.com/acme/job-1/application");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Europe, Remote");
        assertThat(job.salary()).isEqualTo("€150K - €250K");
        assertThat(job.employmentType()).isEqualTo("FullTime");
        assertThat(job.description()).isEqualTo("Build Spring services");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T10:00:00Z");
        assertThat(job.rawData()).containsKey("ashby");
        server.verify();
    }
}
