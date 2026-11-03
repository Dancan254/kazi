package com.kazi.kazi.ingestion.workable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.workable.WorkableProperties.Account;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@SpringJUnitConfig({WorkableJobSource.class, WorkableRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "workable.retry.max-retries=2",
    "workable.retry.delay-ms=1",
    "workable.retry.jitter-ms=0",
    "workable.retry.max-delay-ms=2"
})
class WorkableRetryTest {

    @Autowired
    private WorkableJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(Matchers.containsString("/acme")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.containsString("/acme")))
            .andRespond(withSuccess("{\"name\":\"Acme\",\"jobs\":[]}", MediaType.APPLICATION_JSON));

        try {
            assertThat(jobSource.fetchSince(new Account("Acme", "acme"), null)).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient workableRestClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        WorkableHttpClient workableHttpClient(RestClient workableRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(workableRestClient))
                .build()
                .createClient(WorkableHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
