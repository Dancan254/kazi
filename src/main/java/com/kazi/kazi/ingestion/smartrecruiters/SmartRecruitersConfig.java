package com.kazi.kazi.ingestion.smartrecruiters;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(SmartRecruitersProperties.class)
class SmartRecruitersConfig {

    @Bean
    RestClient smartRecruitersRestClient(RestClient.Builder builder, SmartRecruitersProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Bean
    SmartRecruitersHttpClient smartRecruitersHttpClient(RestClient smartRecruitersRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(smartRecruitersRestClient))
            .build()
            .createClient(SmartRecruitersHttpClient.class);
    }
}
