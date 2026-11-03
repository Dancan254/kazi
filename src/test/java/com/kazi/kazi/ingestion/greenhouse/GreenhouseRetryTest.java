package com.kazi.kazi.ingestion.greenhouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.greenhouse.GreenhouseProperties.Board;
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

@SpringJUnitConfig({GreenhouseJobSource.class, GreenhouseRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "greenhouse.base-url=http://localhost",
    "greenhouse.retry.max-retries=2",
    "greenhouse.retry.delay-ms=1",
    "greenhouse.retry.jitter-ms=0",
    "greenhouse.retry.max-delay-ms=2"
})
class GreenhouseRetryTest {

    @Autowired
    private GreenhouseJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("/acme/jobs")))
            .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/acme/jobs")))
            .andRespond(withSuccess("{\"jobs\":[]}", MediaType.APPLICATION_JSON));

        try {
            var jobs = jobSource.fetchSince(new Board("Acme", "acme"), null);
            assertThat(jobs).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(GreenhouseProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient greenhouseRestClient(RestClient.Builder builder, GreenhouseProperties properties) {
            return builder.baseUrl(properties.baseUrl()).build();
        }

        @Bean
        GreenhouseHttpClient greenhouseHttpClient(RestClient greenhouseRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(greenhouseRestClient))
                .build()
                .createClient(GreenhouseHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
