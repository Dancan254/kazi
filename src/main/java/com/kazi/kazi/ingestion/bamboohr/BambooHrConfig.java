package com.kazi.kazi.ingestion.bamboohr;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(BambooHrProperties.class)
class BambooHrConfig {

    @Bean
    RestClient bambooHrRestClient(RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder.requestFactory(requestFactory).build();
    }

    @Bean
    BambooHrHttpClient bambooHrHttpClient(RestClient bambooHrRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(bambooHrRestClient))
            .build()
            .createClient(BambooHrHttpClient.class);
    }
}
