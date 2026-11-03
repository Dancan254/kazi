package com.kazi.kazi.ingestion.lever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.lever.LeverProperties.Site;
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

@SpringJUnitConfig({LeverJobSource.class, LeverRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "lever.base-url=http://localhost",
    "lever.page-size=100",
    "lever.retry.max-retries=2",
    "lever.retry.delay-ms=1",
    "lever.retry.jitter-ms=0",
    "lever.retry.max-delay-ms=2"
})
class LeverRetryTest {

    @Autowired
    private LeverJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(org.hamcrest.Matchers.containsString("/acme")))
            .andRespond(withServerError());
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/acme")))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        try {
            var jobs = jobSource.fetchSince(new Site("Acme", "acme"), null);
            assertThat(jobs).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(LeverProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient leverRestClient(RestClient.Builder builder, LeverProperties properties) {
            return builder.baseUrl(properties.baseUrl()).build();
        }

        @Bean
        LeverHttpClient leverHttpClient(RestClient leverRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(leverRestClient))
                .build()
                .createClient(LeverHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
