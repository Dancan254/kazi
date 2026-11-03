package com.kazi.kazi.ingestion.recruitee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.recruitee.RecruiteeProperties.Company;
import org.hamcrest.Matchers;
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

@SpringJUnitConfig({RecruiteeJobSource.class, RecruiteeRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "recruitee.base-url=http://localhost/{site}/api/offers/",
    "recruitee.retry.max-retries=2",
    "recruitee.retry.delay-ms=1",
    "recruitee.retry.jitter-ms=0",
    "recruitee.retry.max-delay-ms=2"
})
class RecruiteeRetryTest {

    @Autowired
    private RecruiteeJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(Matchers.containsString("/acme/api/offers/")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.containsString("/acme/api/offers/")))
            .andRespond(withSuccess("{\"offers\":[]}", MediaType.APPLICATION_JSON));

        try {
            assertThat(jobSource.fetchSince(new Company("Acme", "acme"), null)).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(RecruiteeProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient recruiteeRestClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        RecruiteeHttpClient recruiteeHttpClient(RestClient recruiteeRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(recruiteeRestClient))
                .build()
                .createClient(RecruiteeHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
