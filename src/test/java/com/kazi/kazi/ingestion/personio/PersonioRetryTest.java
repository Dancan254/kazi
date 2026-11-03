package com.kazi.kazi.ingestion.personio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.personio.PersonioProperties.Company;
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

@SpringJUnitConfig({PersonioJobSource.class, PersonioRetryTest.ClientConfig.class})
@TestPropertySource(properties = {
    "personio.base-url=http://localhost/{site}/xml",
    "personio.retry.max-retries=2",
    "personio.retry.delay-ms=1",
    "personio.retry.jitter-ms=0",
    "personio.retry.max-delay-ms=2"
})
class PersonioRetryTest {

    @Autowired
    private PersonioJobSource jobSource;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void should_use_spring_retry_to_recover_after_two_failures() {
        server.expect(times(2), requestTo(Matchers.containsString("/acme/xml")))
            .andRespond(withServerError());
        server.expect(requestTo(Matchers.containsString("/acme/xml")))
            .andRespond(withSuccess("<workzag-jobs/>", MediaType.APPLICATION_XML));

        try {
            assertThat(jobSource.fetchSince(new Company("Acme", "acme"), null)).isEmpty();
        } finally {
            server.verify();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableResilientMethods(proxyTargetClass = true)
    @EnableConfigurationProperties(PersonioProperties.class)
    static class ClientConfig {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.DependsOn("mockRestServiceServer")
        RestClient personioRestClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        PersonioHttpClient personioHttpClient(RestClient personioRestClient) {
            return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(personioRestClient))
                .build()
                .createClient(PersonioHttpClient.class);
        }

        @Bean
        MockRestServiceServer mockRestServiceServer(RestClient.Builder builder) {
            return MockRestServiceServer.bindTo(builder).build();
        }
    }
}
