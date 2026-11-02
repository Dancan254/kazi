package com.kazi.kazi.ingestion.adzuna;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("adzuna")
public record AdzunaProperties(
    String baseUrl,
    String appId,
    String appKey,
    String country,
    String currency,
    int resultsPerPage
) {
}
