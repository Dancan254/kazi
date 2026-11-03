package com.kazi.kazi.ingestion.greenhouse;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(GreenhouseProperties.class)
class GreenhouseConfig {

    @Bean
    RestClient greenhouseRestClient(RestClient.Builder builder, GreenhouseProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Bean
    GreenhouseHttpClient greenhouseHttpClient(RestClient greenhouseRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(greenhouseRestClient))
            .build()
            .createClient(GreenhouseHttpClient.class);
    }
}
