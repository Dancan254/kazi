package com.kazi.kazi.ingestion.discovery;

import java.net.HttpURLConnection;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(DiscoveryProperties.class)
class DiscoveryConfig {

    @Bean
    RestClient discoveryRestClient(RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return builder.requestFactory(requestFactory).build();
    }

    @Bean
    DiscoveryHttpClient discoveryHttpClient(RestClient discoveryRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(discoveryRestClient))
            .build()
            .createClient(DiscoveryHttpClient.class);
    }
}
