package com.kazi.kazi.ingestion.lever;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lever")
public record LeverProperties(
    String baseUrl,
    int pageSize,
    List<Site> sites
) {
    public LeverProperties {
        sites = sites == null ? List.of() : sites.stream()
            .filter(site -> site.company() != null && !site.company().isBlank())
            .filter(site -> site.site() != null && !site.site().isBlank())
            .toList();
    }

    public record Site(String company, String site) {
    }
}
