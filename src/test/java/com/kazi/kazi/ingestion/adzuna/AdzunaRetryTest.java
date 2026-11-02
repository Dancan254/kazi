package com.kazi.kazi.ingestion.adzuna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.source.JobSource;
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

@SpringJUnitConfig({AdzunaJobSource.class, AdzunaRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "adzuna.base-url=http://localhost",
    "adzuna.app-id=test-id",
    "adzuna.app-key=test-key",
    "adzuna.country=us",
    "adzuna.currency=USD",
    "adzuna.results-per-page=50",
    "adzuna.retry.max-retries=2",
    "adzuna.retry.delay-ms=1",
    "adzuna.retry.jitter-ms=0",
    "adzuna.retry.max-delay-ms=2"
})
class AdzunaRetryTest {

    @Autowired
    private JobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("/jobs/us/search/1"))).andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/jobs/us/search/1"))).andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        try {
            var jobs = jobSource.fetchSince(null);
            assertThat(jobs).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(AdzunaProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient adzunaRestClient(RestClient.Builder builder, AdzunaProperties properties) {
            return builder.baseUrl(properties.baseUrl()).build();
        }

        @Bean
        AdzunaHttpClient adzunaHttpClient(RestClient adzunaRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(adzunaRestClient))
                .build()
                .createClient(AdzunaHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
