package com.kazi.kazi.ingestion.smartrecruiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersProperties.Company;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
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

@SpringJUnitConfig({
    SmartRecruitersClient.class,
    SmartRecruitersJobSource.class,
    SmartRecruitersRetryTest.ClientConfig.class
})
@TestPropertySource(properties = {
    "smartrecruiters.base-url=http://localhost/v1/companies",
    "smartrecruiters.retry.max-retries=2",
    "smartrecruiters.retry.delay-ms=1",
    "smartrecruiters.retry.jitter-ms=0",
    "smartrecruiters.retry.max-delay-ms=2"
})
class SmartRecruitersRetryTest {

    @Autowired
    private SmartRecruitersJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void resetServer() {
        server.reset();
    }

    @Test
    void should_recover_after_two_failed_list_requests() {
        server.expect(times(2), requestTo(Matchers.containsString("/acme/postings")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.containsString("/acme/postings")))
            .andRespond(withSuccess("{\"totalFound\":0,\"content\":[]}", MediaType.APPLICATION_JSON));

        try {
            assertThat(jobSource.fetchSince(new Company("Acme", "acme"), null)).isEmpty();
        } finally {
            server.verify();
        }
    }

    @Test
    void should_retry_only_the_failed_detail_request() {
        server.expect(requestTo(Matchers.containsString("/acme/postings?")))
            .andRespond(withSuccess("""
                {"totalFound":1,"content":[{"id":"job-1","name":"Java Engineer"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(times(2), requestTo(Matchers.endsWith("/acme/postings/job-1")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.endsWith("/acme/postings/job-1")))
            .andRespond(withSuccess("""
                {"id":"job-1","name":"Java Engineer","postingUrl":"https://example.test/job-1"}
                """, MediaType.APPLICATION_JSON));

        try {
            assertThat(jobSource.fetchSince(new Company("Acme", "acme"), null)).hasSize(1);
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(SmartRecruitersProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient smartRecruitersRestClient(RestClient.Builder builder) {
            return builder.baseUrl("http://localhost/v1/companies").build();
        }

        @Bean
        SmartRecruitersHttpClient smartRecruitersHttpClient(RestClient smartRecruitersRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(smartRecruitersRestClient))
                .build()
                .createClient(SmartRecruitersHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
