package com.kazi.kazi.ingestion.workable;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(WorkableProperties.class)
class WorkableConfig {

    @Bean
    RestClient workableRestClient(RestClient.Builder builder, WorkableProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Bean
    WorkableHttpClient workableHttpClient(RestClient workableRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(workableRestClient))
            .build()
            .createClient(WorkableHttpClient.class);
    }
}
