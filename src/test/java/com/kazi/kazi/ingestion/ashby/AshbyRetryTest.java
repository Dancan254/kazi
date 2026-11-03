package com.kazi.kazi.ingestion.ashby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.ashby.AshbyProperties.Organization;
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

@SpringJUnitConfig({AshbyJobSource.class, AshbyRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "ashby.base-url=http://localhost",
    "ashby.retry.max-retries=2",
    "ashby.retry.delay-ms=1",
    "ashby.retry.jitter-ms=0",
    "ashby.retry.max-delay-ms=2"
})
class AshbyRetryTest {

    @Autowired
    private AshbyJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("/acme")))
            .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/acme")))
            .andRespond(withSuccess("{\"jobs\":[]}", MediaType.APPLICATION_JSON));

        try {
            var jobs = jobSource.fetchSince(new Organization("Acme", "acme"), null);
            assertThat(jobs).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(AshbyProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient ashbyRestClient(RestClient.Builder builder, AshbyProperties properties) {
            return builder.baseUrl(properties.baseUrl()).build();
        }

        @Bean
        AshbyHttpClient ashbyHttpClient(RestClient ashbyRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(ashbyRestClient))
                .build()
                .createClient(AshbyHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
