package com.kazi.kazi.ingestion.workday;

import java.time.Duration;
import java.util.ArrayList;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(WorkdayProperties.class)
class WorkdayConfig {

    @Bean
    RestClient workdayRestClient(RestClient.Builder builder) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        builder.requestFactory(requestFactory);
        configureJsonMediaTypes(builder);
        return builder.build();
    }

    static void configureJsonMediaTypes(RestClient.Builder builder) {
        builder.configureMessageConverters(converters -> converters
            .registerDefaults()
            .configureMessageConverters(converter -> {
                if (converter instanceof JacksonJsonHttpMessageConverter jsonConverter) {
                    var mediaTypes = new ArrayList<>(jsonConverter.getSupportedMediaTypes());
                    mediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
                    jsonConverter.setSupportedMediaTypes(mediaTypes);
                }
            }));
    }

    @Bean
    WorkdayHttpClient workdayHttpClient(RestClient workdayRestClient) {
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(workdayRestClient))
            .build()
            .createClient(WorkdayHttpClient.class);
    }
}
