package com.kazi.kazi.ingestion.bamboohr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.bamboohr.BambooHrProperties.Company;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class BambooHrJobSourceTest {

    private final BambooHrJobSource jobSource;
    private final MockRestServiceServer server;

    BambooHrJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        BambooHrHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(BambooHrHttpClient.class);
        jobSource = new BambooHrJobSource(
            new BambooHrClient(httpClient),
            new BambooHrProperties("http://localhost/{site}/careers", null)
        );
    }

    @Test
    void should_map_bamboohr_listing_and_detail() {
        server.expect(requestTo(containsString("/acme/careers/list")))
            .andRespond(withSuccess("""
                {
                  "meta": {"totalCount": 1},
                  "result": [{
                    "id": "35",
                    "jobOpeningName": "Infrastructure Engineer",
                    "employmentStatusLabel": "Full-time",
                    "isRemote": true,
                    "locationType": "1"
                  }]
                }
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/acme/careers/35/detail")))
            .andRespond(withSuccess("""
                {
                  "result": {"jobOpening": {
                    "jobOpeningShareUrl": "https://acme.bamboohr.com/careers/35",
                    "jobOpeningName": "Infrastructure Engineer",
                    "employmentStatusLabel": "Full-time",
                    "location": {"city": "Nairobi", "addressCountry": "Kenya"},
                    "description": "<p>Operate cloud services</p>",
                    "compensation": "$90k to $200k USD",
                    "datePosted": "2026-11-02",
                    "locationType": "1"
                  }}
                }
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(new Company("Acme", "acme"), null);

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("bamboohr:acme");
        assertThat(job.sourceJobId()).isEqualTo("35");
        assertThat(job.sourceUrl()).isEqualTo("https://acme.bamboohr.com/careers/35");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Nairobi, Kenya, Remote");
        assertThat(job.salary()).isEqualTo("$90k to $200k USD");
        assertThat(job.employmentType()).isEqualTo("Full-time");
        assertThat(job.description()).isEqualTo("<p>Operate cloud services</p>");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T00:00:00Z");
        assertThat(job.rawData()).containsKey("bamboohr");
        server.verify();
    }
}
