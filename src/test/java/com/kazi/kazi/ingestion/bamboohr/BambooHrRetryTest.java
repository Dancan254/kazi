package com.kazi.kazi.ingestion.bamboohr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.bamboohr.BambooHrProperties.Company;
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

@SpringJUnitConfig({BambooHrClient.class, BambooHrJobSource.class, BambooHrRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "bamboohr.base-url=http://localhost/{site}/careers",
    "bamboohr.retry.max-retries=2",
    "bamboohr.retry.delay-ms=1",
    "bamboohr.retry.jitter-ms=0",
    "bamboohr.retry.max-delay-ms=2"
})
class BambooHrRetryTest {

    @Autowired
    private BambooHrJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void resetServer() {
        server.reset();
    }

    @Test
    void should_recover_after_two_failed_list_requests() {
        server.expect(times(2), requestTo(Matchers.containsString("/acme/careers/list")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.containsString("/acme/careers/list")))
            .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));

        try {
            assertThat(jobSource.fetchSince(new Company("Acme", "acme"), null)).isEmpty();
        } finally {
            server.verify();
        }
    }

    @Test
    void should_retry_only_the_failed_detail_request() {
        server.expect(requestTo(Matchers.containsString("/acme/careers/list")))
            .andRespond(withSuccess("""
                {"result":[{"id":"35","jobOpeningName":"Java Engineer"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(times(2), requestTo(Matchers.containsString("/acme/careers/35/detail")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.containsString("/acme/careers/35/detail")))
            .andRespond(withSuccess("""
                {"result":{"jobOpening":{"jobOpeningName":"Java Engineer","jobOpeningShareUrl":"https://example.test/35"}}}
                """, MediaType.APPLICATION_JSON));

        try {
            assertThat(jobSource.fetchSince(new Company("Acme", "acme"), null)).hasSize(1);
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(BambooHrProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient bambooHrRestClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        BambooHrHttpClient bambooHrHttpClient(RestClient bambooHrRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(bambooHrRestClient))
                .build()
                .createClient(BambooHrHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
