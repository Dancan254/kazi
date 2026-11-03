package com.kazi.kazi.ingestion.discovery;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discovery")
public record DiscoveryProperties(List<Site> sites) {

    public DiscoveryProperties {
        sites = sites == null ? List.of() : sites.stream()
            .filter(site -> hasText(site.company()))
            .filter(site -> hasText(site.url()))
            .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Site(String company, String url) {
    }
}
