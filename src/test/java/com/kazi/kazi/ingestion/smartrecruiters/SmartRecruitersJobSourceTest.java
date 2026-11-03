package com.kazi.kazi.ingestion.smartrecruiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersProperties.Company;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class SmartRecruitersJobSourceTest {

    private final SmartRecruitersJobSource jobSource;
    private final MockRestServiceServer server;

    SmartRecruitersJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://localhost/v1/companies").build();
        SmartRecruitersHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(SmartRecruitersHttpClient.class);
        jobSource = new SmartRecruitersJobSource(
            new SmartRecruitersClient(httpClient),
            new SmartRecruitersProperties("http://localhost/v1/companies", 1, null)
        );
    }

    @Test
    void should_page_and_map_smartrecruiters_postings() {
        expectPage(0, "job-1", "Java Engineer");
        expectDetail("job-1", "Java Engineer", "<p>Build Spring services</p>");
        expectPage(1, "job-2", "Platform Engineer");
        expectDetail("job-2", "Platform Engineer", "<p>Operate cloud services</p>");

        var jobs = jobSource.fetchSince(new Company("Acme", "acme"), null);

        assertThat(jobs).hasSize(2);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("smartrecruiters:acme");
        assertThat(job.sourceJobId()).isEqualTo("job-1");
        assertThat(job.sourceUrl()).isEqualTo("https://jobs.smartrecruiters.com/acme/job-1?oga=true");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Nairobi, Kenya");
        assertThat(job.employmentType()).isEqualTo("Full-time");
        assertThat(job.description()).contains("Build Spring services", "Java and Spring experience");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T10:00:00Z");
        assertThat(job.rawData()).containsKey("smartrecruiters");
        server.verify();
    }

    private void expectPage(int offset, String id, String title) {
        server.expect(requestTo(containsString("/acme/postings")))
            .andExpect(queryParam("limit", "1"))
            .andExpect(queryParam("offset", String.valueOf(offset)))
            .andRespond(withSuccess("""
                {
                  "offset": %d,
                  "limit": 1,
                  "totalFound": 2,
                  "content": [{
                    "id": "%s",
                    "name": "%s",
                    "releasedDate": "2026-11-02T10:00:00Z",
                    "location": {"city": "Nairobi", "country": "Kenya", "remote": false},
                    "typeOfEmployment": {"label": "Full-time"}
                  }]
                }
                """.formatted(offset, id, title), MediaType.APPLICATION_JSON));
    }

    private void expectDetail(String id, String title, String description) {
        server.expect(requestTo(containsString("/acme/postings/" + id)))
            .andRespond(withSuccess("""
                {
                  "id": "%s",
                  "name": "%s",
                  "releasedDate": "2026-11-02T10:00:00Z",
                  "postingUrl": "https://jobs.smartrecruiters.com/acme/%s",
                  "applyUrl": "https://jobs.smartrecruiters.com/acme/%s?oga=true",
                  "location": {"fullLocation": "Nairobi, Kenya", "remote": false},
                  "typeOfEmployment": {"label": "Full-time"},
                  "jobAd": {"sections": {
                    "jobDescription": {"text": "%s"},
                    "qualifications": {"text": "<p>Java and Spring experience</p>"}
                  }}
                }
                """.formatted(id, title, id, id, description), MediaType.APPLICATION_JSON));
    }
}
