package com.kazi.kazi.ingestion.workday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.workday.WorkdayProperties.Site;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@SpringJUnitConfig({WorkdayClient.class, WorkdayJobSource.class, WorkdayRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "workday.retry.max-retries=2",
    "workday.retry.delay-ms=1",
    "workday.retry.jitter-ms=0",
    "workday.retry.max-delay-ms=2"
})
class WorkdayRetryTest {

    @Autowired
    private WorkdayJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @org.junit.jupiter.api.BeforeEach
    void resetServer() {
        server.reset();
    }

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("/acme/External/jobs")))
            .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/acme/External/jobs")))
            .andRespond(withSuccess("{\"total\":0,\"jobPostings\":[]}", MediaType.APPLICATION_JSON));

        try {
            var jobs = jobSource.fetchSince(new Site("Acme", "http://localhost", "acme", "External"), null);
            assertThat(jobs).isEmpty();
        } finally {
            server.verify();
        }
    }

    @Test
    void should_retry_only_the_failed_detail_request() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/acme/External/jobs")))
            .andRespond(withSuccess("""
                {
                  "total": 1,
                  "jobPostings": [{
                    "title": "Java Engineer",
                    "externalPath": "/job/JR-1"
                  }]
                }
                """, MediaType.APPLICATION_JSON));
        server.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("/acme/External/job/JR-1")))
            .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/acme/External/job/JR-1")))
            .andRespond(withSuccess("""
                {
                  "jobPostingInfo": {
                    "jobReqId": "JR-1",
                    "title": "Java Engineer",
                    "jobDescription": "<p>Build Spring services</p>"
                  }
                }
                """, MediaType.APPLICATION_JSON));

        try {
            var jobs = jobSource.fetchSince(new Site("Acme", "http://localhost", "acme", "External"), null);
            assertThat(jobs).hasSize(1);
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(WorkdayProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient workdayRestClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        WorkdayHttpClient workdayHttpClient(RestClient workdayRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(workdayRestClient))
                .build()
                .createClient(WorkdayHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
