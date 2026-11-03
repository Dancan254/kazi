package com.kazi.kazi.ingestion.personio;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(PersonioProperties.class)
class PersonioConfig {

    @Bean
    RestClient personioRestClient(RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder.requestFactory(requestFactory).build();
    }

    @Bean
    PersonioHttpClient personioHttpClient(RestClient personioRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(personioRestClient))
            .build()
            .createClient(PersonioHttpClient.class);
    }
}
