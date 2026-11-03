package com.kazi.kazi.ingestion.recruitee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.recruitee.RecruiteeProperties.Company;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class RecruiteeJobSourceTest {

    private final RecruiteeJobSource jobSource;
    private final MockRestServiceServer server;

    RecruiteeJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        RecruiteeHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(RecruiteeHttpClient.class);
        jobSource = new RecruiteeJobSource(
            httpClient,
            new RecruiteeProperties("http://localhost/{site}/api/offers/", null)
        );
    }

    @Test
    void should_map_recruitee_offers() {
        server.expect(requestTo(containsString("/acme/api/offers/")))
            .andRespond(withSuccess("""
                {
                  "offers": [{
                    "position": 1006,
                    "guid": "gy8uf",
                    "title": "Senior Java Engineer",
                    "location": "Rotterdam, Netherlands",
                    "remote": true,
                    "employment_type_code": "fulltime_fixed_term",
                    "published_at": "2026-11-02 10:09:59 UTC",
                    "careers_url": "https://acme.recruitee.com/o/senior-java-engineer",
                    "description": "<p>Build Spring services</p>",
                    "requirements": "<p>Java experience</p>",
                    "salary": {"min": 5000, "max": 7000, "period": "month", "currency": "EUR"},
                    "locations": [{"city": "Rotterdam", "state": "Zuid-Holland", "country": "Netherlands"}]
                  }]
                }
                """, MediaType.APPLICATION_JSON));

        var jobs = jobSource.fetchSince(new Company("Acme", "acme"), null);

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("recruitee:acme");
        assertThat(job.sourceJobId()).isEqualTo("gy8uf");
        assertThat(job.sourceUrl()).isEqualTo("https://acme.recruitee.com/o/senior-java-engineer");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Rotterdam, Zuid-Holland, Netherlands, Remote");
        assertThat(job.salary()).isEqualTo("EUR 5000 - 7000 per month");
        assertThat(job.employmentType()).isEqualTo("Full-time");
        assertThat(job.description()).contains("Build Spring services", "Java experience");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T10:09:59Z");
        assertThat(job.rawData()).containsKey("recruitee");
        server.verify();
    }
}
