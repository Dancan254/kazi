package com.kazi.kazi.ingestion.workday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.workday.WorkdayProperties.Site;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class WorkdayJobSourceTest {

    private final WorkdayJobSource jobSource;
    private final MockRestServiceServer server;

    WorkdayJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        WorkdayConfig.configureJsonMediaTypes(builder);
        RestClient restClient = builder.build();
        WorkdayHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(WorkdayHttpClient.class);
        jobSource = new WorkdayJobSource(new WorkdayClient(httpClient));
    }

    @Test
    void should_read_json_with_octet_stream_content_type() {
        server.expect(requestTo("http://acme.test/wday/cxs/acme/External/jobs"))
            .andRespond(withSuccess(
                "{\"total\":0,\"jobPostings\":[]}",
                MediaType.APPLICATION_OCTET_STREAM
            ));

        var jobs = jobSource.fetchSince(new Site("Acme", "http://acme.test", "acme", "External"), null);

        assertThat(jobs).isEmpty();
        server.verify();
    }

    @Test
    void should_fetch_all_pages_and_map_workday_posting_details() {
        expectJobsPage(0, """
            {
              "total": 21,
              "jobPostings": [{
                "title": "Senior Java Engineer",
                "externalPath": "/job/USA/Senior-Java-Engineer_JR-1",
                "locationsText": "2 Locations",
                "postedOn": "Posted Today",
                "remoteType": "Remote",
                "bulletFields": ["JR-1"]
              }]
            }
            """);
        expectJobDetail("/job/USA/Senior-Java-Engineer_JR-1", """
            {
              "jobPostingInfo": {
                "jobReqId": "JR-1",
                "title": "Senior Java Engineer",
                "location": "USA, California, San Francisco",
                "startDate": "2026-11-02",
                "timeType": "Full Time",
                "jobDescription": "<p>Build Spring services</p>",
                "externalUrl": "https://careers.example/jobs/JR-1",
                "additionalLocations": ["USA, New York, New York"]
              }
            }
            """);
        expectJobsPage(20, """
            {
              "total": 0,
              "jobPostings": [{
                "title": "Platform Engineer",
                "externalPath": "/job/Kenya/Platform-Engineer_JR-2",
                "locationsText": "Nairobi, Kenya",
                "postedOn": "Posted Yesterday",
                "bulletFields": ["JR-2"]
              }]
            }
            """);
        expectJobDetail("/job/Kenya/Platform-Engineer_JR-2", """
            {
              "jobPostingInfo": {
                "jobReqId": "JR-2",
                "title": "Platform Engineer",
                "location": "Nairobi, Kenya",
                "startDate": "2026-11-01",
                "timeType": "Contract",
                "jobDescription": "<p>Build platforms</p>",
                "externalUrl": "https://careers.example/jobs/JR-2"
              }
            }
            """);
        var jobs = jobSource.fetchSince(
            new Site("Acme", "http://acme.test", "acme", "External"),
            Instant.parse("2026-12-01T00:00:00Z")
        );

        assertThat(jobs).hasSize(2);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("workday:acme/External");
        assertThat(job.sourceJobId()).isEqualTo("/job/USA/Senior-Java-Engineer_JR-1");
        assertThat(job.sourceUrl()).isEqualTo("https://careers.example/jobs/JR-1");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location())
            .isEqualTo("USA, California, San Francisco, USA, New York, New York, Remote");
        assertThat(job.employmentType()).isEqualTo("Full Time");
        assertThat(job.description()).isEqualTo("<p>Build Spring services</p>");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T00:00:00Z");
        assertThat(job.rawData()).containsKey("workday");
        server.verify();
    }

    private void expectJobsPage(int offset, String response) {
        server.expect(requestTo("http://acme.test/wday/cxs/acme/External/jobs"))
            .andExpect(jsonPath("$.appliedFacets").isEmpty())
            .andExpect(jsonPath("$.limit").value(20))
            .andExpect(jsonPath("$.offset").value(offset))
            .andExpect(jsonPath("$.searchText").value(""))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private void expectJobDetail(String path, String response) {
        server.expect(requestTo("http://acme.test/wday/cxs/acme/External" + path))
            .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }
}
