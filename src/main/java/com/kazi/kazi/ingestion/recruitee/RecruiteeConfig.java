package com.kazi.kazi.ingestion.recruitee;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(RecruiteeProperties.class)
class RecruiteeConfig {

    @Bean
    RestClient recruiteeRestClient(RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder.requestFactory(requestFactory).build();
    }

    @Bean
    RecruiteeHttpClient recruiteeHttpClient(RestClient recruiteeRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(recruiteeRestClient))
            .build()
            .createClient(RecruiteeHttpClient.class);
    }
}
